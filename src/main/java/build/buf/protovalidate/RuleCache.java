// Copyright 2023-2024 Buf Technologies, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package build.buf.protovalidate;

import build.buf.protovalidate.exceptions.CompilationException;
import build.buf.validate.FieldPath;
import build.buf.validate.FieldRules;
import build.buf.validate.ValidateProto;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageLite;
import com.google.protobuf.TypeRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.Nullable;
import org.projectnessie.cel.Ast;
import org.projectnessie.cel.Env;
import org.projectnessie.cel.EnvOption;
import org.projectnessie.cel.EvalOption;
import org.projectnessie.cel.Program;
import org.projectnessie.cel.ProgramOption;
import org.projectnessie.cel.checker.Decls;
import org.projectnessie.cel.common.types.ref.Val;
import org.projectnessie.cel.interpreter.Activation;

/** A build-through cache for computed standard rules. */
class RuleCache {
  private static class CelRule {
    public final AstExpression astExpression;
    public final FieldDescriptor field;
    public final FieldPath rulePath;

    public CelRule(AstExpression astExpression, FieldDescriptor field, FieldPath rulePath) {
      this.astExpression = astExpression;
      this.field = field;
      this.rulePath = rulePath;
    }
  }

  private static final ExtensionRegistry EXTENSION_REGISTRY = ExtensionRegistry.newInstance();

  static {
    EXTENSION_REGISTRY.add(ValidateProto.predefined);
  }

  /** Partial eval options for evaluating the rule's expression. */
  private static final ProgramOption PARTIAL_EVAL_OPTIONS =
      ProgramOption.evalOptions(
          EvalOption.OptTrackState,
          EvalOption.OptExhaustiveEval,
          EvalOption.OptOptimize,
          EvalOption.OptPartialEval);

  /**
   * Concurrent map for caching {@link FieldDescriptor} and their associated List of {@link
   * AstExpression}.
   */
  private static final Map<FieldDescriptor, List<CelRule>> descriptorMap =
      new ConcurrentHashMap<>();

  /** The environment to use for evaluation. */
  private final Env env;

  /** Registry used to resolve dynamic messages. */
  private final TypeRegistry typeRegistry;

  /** Registry used to resolve dynamic extensions. */
  private final ExtensionRegistry extensionRegistry;

  /** Whether to allow unknown rule fields or not. */
  private final boolean allowUnknownFields;

  /**
   * Constructs a new build-through cache for the standard rules, with a provided registry to
   * resolve dynamic extensions.
   *
   * @param env The CEL environment for evaluation.
   * @param config The configuration to use for the rule cache.
   */
  public RuleCache(Env env, Config config) {
    this.env = env;
    this.typeRegistry = config.getTypeRegistry();
    this.extensionRegistry = config.getExtensionRegistry();
    this.allowUnknownFields = config.isAllowingUnknownFields();
  }

  /**
   * Creates the standard rules for the given field. If forItems is true, the rules for repeated
   * list items is built instead of the rules on the list itself.
   *
   * @param fieldDescriptor The field descriptor to be validated.
   * @param fieldRules The field rule that is used for validation.
   * @param forItems The field is an item list type.
   * @return The list of compiled programs.
   * @throws CompilationException If the rules fail to compile.
   */
  public List<CompiledProgram> compile(
      FieldDescriptor fieldDescriptor, FieldRules fieldRules, boolean forItems)
      throws CompilationException {
    ResolvedRule resolved = resolveRules(fieldDescriptor, fieldRules, forItems);
    if (resolved == null) {
      // Message null means there were no rules resolved.
      return Collections.emptyList();
    }
    Message message = resolved.message;
    List<CelRule> completeProgramList = new ArrayList<>();
    for (Map.Entry<FieldDescriptor, Object> entry : message.getAllFields().entrySet()) {
      FieldDescriptor ruleFieldDesc = entry.getKey();
      List<CelRule> programList =
          compileRule(fieldDescriptor, forItems, resolved.setOneof, ruleFieldDesc, message);
      if (programList == null) continue;
      completeProgramList.addAll(programList);
    }
    List<CompiledProgram> programs = new ArrayList<>();
    for (CelRule rule : completeProgramList) {
      Env ruleEnv = getRuleEnv(fieldDescriptor, message, rule.field, forItems);
      Variable ruleVar = Variable.newRuleVariable(message, message.getField(rule.field));
      ProgramOption globals = ProgramOption.globals(ruleVar);
      Value ruleValue = new ObjectValue(rule.field, message.getField(rule.field));
      try {
        Program program = ruleEnv.program(rule.astExpression.ast, globals, PARTIAL_EVAL_OPTIONS);
        Program.EvalResult evalResult = program.eval(Activation.emptyActivation());
        Val value = evalResult.getVal();
        if (value != null) {
          Object val = value.value();
          if (val instanceof Boolean && value.booleanValue()) {
            continue;
          }
          if (val instanceof String && val.equals("")) {
            continue;
          }
        }
        Ast residual = ruleEnv.residualAst(rule.astExpression.ast, evalResult.getEvalDetails());
        programs.add(
            new CompiledProgram(
                ruleEnv.program(residual, globals),
                rule.astExpression.source,
                rule.rulePath,
                ruleValue));
      } catch (Exception e) {
        programs.add(
            new CompiledProgram(
                ruleEnv.program(rule.astExpression.ast, globals),
                rule.astExpression.source,
                rule.rulePath,
                ruleValue));
      }
    }
    return Collections.unmodifiableList(programs);
  }

  private @Nullable List<CelRule> compileRule(
      FieldDescriptor fieldDescriptor,
      boolean forItems,
      FieldDescriptor setOneof,
      FieldDescriptor ruleFieldDesc,
      Message message)
      throws CompilationException {
    List<CelRule> celRules = descriptorMap.get(fieldDescriptor);
    if (celRules != null) {
      return celRules;
    }
    build.buf.validate.PredefinedRules rules = getFieldRules(ruleFieldDesc);
    if (rules == null) return null;
    List<Expression> expressions = Expression.fromRules(rules.getCelList());
    celRules = new ArrayList<>(expressions.size());
    Env ruleEnv = getRuleEnv(fieldDescriptor, message, ruleFieldDesc, forItems);
    for (Expression expression : expressions) {
      FieldPath rulePath =
          FieldPath.newBuilder()
              .addElements(FieldPathUtils.fieldPathElement(setOneof))
              .addElements(FieldPathUtils.fieldPathElement(ruleFieldDesc))
              .build();
      celRules.add(
          new CelRule(
              AstExpression.newAstExpression(ruleEnv, expression), ruleFieldDesc, rulePath));
    }
    descriptorMap.put(ruleFieldDesc, celRules);
    return celRules;
  }

  private build.buf.validate.@Nullable PredefinedRules getFieldRules(FieldDescriptor ruleFieldDesc)
      throws CompilationException {
    DescriptorProtos.FieldOptions options = ruleFieldDesc.getOptions();
    // If the protovalidate field option is unknown, reparse options using our extension registry.
    if (options.getUnknownFields().hasField(ValidateProto.predefined.getNumber())) {
      try {
        options =
            DescriptorProtos.FieldOptions.parseFrom(options.toByteString(), EXTENSION_REGISTRY);
      } catch (InvalidProtocolBufferException e) {
        throw new CompilationException("Failed to parse field options", e);
      }
    }
    if (!options.hasExtension(ValidateProto.predefined)) {
      return null;
    }
    Object extensionValue = options.getField(ValidateProto.predefined.getDescriptor());
    build.buf.validate.PredefinedRules rules;
    if (extensionValue instanceof build.buf.validate.PredefinedRules) {
      rules = (build.buf.validate.PredefinedRules) extensionValue;
    } else if (extensionValue instanceof MessageLite) {
      // Extension is parsed but with different gencode. We need to reparse it.
      try {
        rules =
            build.buf.validate.PredefinedRules.parseFrom(
                ((MessageLite) extensionValue).toByteString());
      } catch (InvalidProtocolBufferException e) {
        throw new CompilationException("Failed to parse field rules", e);
      }
    } else {
      // Extension was not a message, just discard it.
      return null;
    }
    return rules;
  }

  /**
   * Calculates the environment for a specific rule invocation.
   *
   * @param fieldDescriptor The field descriptor of the field with the rule.
   * @param ruleMessage The message of the standard rules.
   * @param ruleFieldDesc The field descriptor of the rule.
   * @param forItems Whether the field is a list type or not.
   * @return An environment with requisite declarations and types added.
   */
  private Env getRuleEnv(
      FieldDescriptor fieldDescriptor,
      Message ruleMessage,
      FieldDescriptor ruleFieldDesc,
      boolean forItems) {
    return env.extend(
        EnvOption.types(ruleMessage.getDefaultInstanceForType()),
        EnvOption.declarations(
            Decls.newVar(
                Variable.THIS_NAME, DescriptorMappings.getCELType(fieldDescriptor, forItems)),
            Decls.newVar(
                Variable.RULES_NAME,
                Decls.newObjectType(ruleMessage.getDescriptorForType().getFullName())),
            Decls.newVar(Variable.RULE_NAME, DescriptorMappings.getCELType(ruleFieldDesc, false))));
  }

  private static class ResolvedRule {
    final Message message;
    final FieldDescriptor setOneof;

    ResolvedRule(Message message, FieldDescriptor setOneof) {
      this.message = message;
      this.setOneof = setOneof;
    }
  }

  /**
   * Extracts the standard rules for the specified field. An exception is thrown if the wrong rules
   * are applied to a field (typically if there is a type-mismatch). Null is returned if there are
   * no standard rules to apply to this field.
   */
  @Nullable
  private ResolvedRule resolveRules(
      FieldDescriptor fieldDescriptor, FieldRules fieldRules, boolean forItems)
      throws CompilationException {
    // Get the oneof field descriptor from the field rules.
    FieldDescriptor oneofFieldDescriptor =
        fieldRules.getOneofFieldDescriptor(DescriptorMappings.FIELD_RULES_ONEOF_DESC);
    if (oneofFieldDescriptor == null) {
      // If the oneof field descriptor is null there are no rules to resolve.
      return null;
    }

    // Get the expected rule descriptor based on the provided field descriptor and the flag
    // indicating whether it is for items.
    FieldDescriptor expectedRuleDescriptor =
        DescriptorMappings.getExpectedRuleDescriptor(fieldDescriptor, forItems);

    if (expectedRuleDescriptor != null
        && !oneofFieldDescriptor.getFullName().equals(expectedRuleDescriptor.getFullName())) {
      // If the expected rule does not match the actual oneof rule, throw a
      // CompilationError.
      throw new CompilationException(
          String.format(
              "expected rule %s, got %s on field %s",
              expectedRuleDescriptor.getName(),
              oneofFieldDescriptor.getName(),
              fieldDescriptor.getName()));
    }

    // If the expected rule descriptor is null or if the field rules do not have the
    // oneof field descriptor there are no rules to resolve, so return null.
    if (expectedRuleDescriptor == null || !fieldRules.hasField(oneofFieldDescriptor)) {
      if (expectedRuleDescriptor == null) {
        // The only expected rule descriptor for message fields is for well known types.
        // If we didn't find a descriptor and this is a message, there must be a mismatch.
        if (fieldDescriptor.getJavaType() == FieldDescriptor.JavaType.MESSAGE) {
          throw new CompilationException(
              String.format(
                  "mismatched message rules, %s is not a valid rule for field %s",
                  oneofFieldDescriptor.getName(), fieldDescriptor.getName()));
        }
      }

      return null;
    }

    // Get the field from the field rules identified by the oneof field descriptor, casted
    // as a Message.
    Message typeRules = (Message) fieldRules.getField(oneofFieldDescriptor);
    if (!typeRules.getUnknownFields().isEmpty()) {
      // If there are unknown fields, try to resolve them using the provided registries. Note that
      // we use the type registry to resolve the message descriptor. This is because Java protobuf
      // extension resolution relies on descriptor identity. The user's provided type registry can
      // provide matching message descriptors for the user's provided extension registry. See the
      // documentation for Options.setTypeRegistry for more information.
      Descriptors.Descriptor expectedRuleMessageDescriptor =
          typeRegistry.find(expectedRuleDescriptor.getMessageType().getFullName());
      if (expectedRuleMessageDescriptor == null) {
        expectedRuleMessageDescriptor = expectedRuleDescriptor.getMessageType();
      }
      try {
        typeRules =
            DynamicMessage.parseFrom(
                expectedRuleMessageDescriptor, typeRules.toByteString(), extensionRegistry);
      } catch (InvalidProtocolBufferException e) {
        throw new RuntimeException(e);
      }
    }
    if (!allowUnknownFields && !typeRules.getUnknownFields().isEmpty()) {
      throw new CompilationException("unrecognized field rules");
    }
    return new ResolvedRule(typeRules, oneofFieldDescriptor);
  }
}
