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
import build.buf.validate.FieldPathElement;
import build.buf.validate.FieldRules;
import build.buf.validate.Ignore;
import build.buf.validate.MessageRules;
import build.buf.validate.OneofRules;
import build.buf.validate.Rule;
import com.google.api.expr.v1alpha1.Decl;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.projectnessie.cel.Env;
import org.projectnessie.cel.EnvOption;
import org.projectnessie.cel.checker.Decls;

/** A build-through cache of message evaluators keyed off the provided descriptor. */
class EvaluatorBuilder {
  private static final FieldPathElement CEL_FIELD_PATH_ELEMENT =
      FieldPathUtils.fieldPathElement(
          FieldRules.getDescriptor().findFieldByNumber(FieldRules.CEL_FIELD_NUMBER));

  private volatile Map<Descriptor, MessageEvaluator> evaluatorCache = Collections.emptyMap();

  private final Env env;
  private final boolean disableLazy;
  private final RuleCache rules;

  /**
   * Constructs a new {@link EvaluatorBuilder}.
   *
   * @param env The CEL environment for evaluation.
   * @param config The configuration to use for the evaluation.
   */
  public EvaluatorBuilder(Env env, Config config) {
    this.env = env;
    this.disableLazy = config.isDisableLazy();
    this.rules = new RuleCache(env, config);
  }

  /**
   * Returns a pre-cached {@link Evaluator} for the given descriptor or, if the descriptor is
   * unknown, returns an evaluator that always throws a {@link CompilationException}.
   *
   * @param desc Protobuf descriptor type.
   * @return An evaluator for the descriptor type.
   * @throws CompilationException If an evaluator can't be created for the specified descriptor.
   */
  public Evaluator load(Descriptor desc) throws CompilationException {
    Evaluator evaluator = evaluatorCache.get(desc);
    if (evaluator == null && disableLazy) {
      return new UnknownDescriptorEvaluator(desc);
    }
    return build(desc);
  }

  /**
   * Either returns a memoized {@link Evaluator} for the given descriptor, or lazily constructs a
   * new one.
   */
  private Evaluator build(Descriptor desc) throws CompilationException {
    Evaluator eval = evaluatorCache.get(desc);
    if (eval != null) {
      return eval;
    }
    synchronized (this) {
      // Check again (we may have lost race with another thread which populated the map with this
      // descriptor).
      eval = evaluatorCache.get(desc);
      if (eval != null) {
        return eval;
      }
      // Rebuild cache with this descriptor (and any of its dependencies).
      Map<Descriptor, MessageEvaluator> updatedCache =
          new DescriptorCacheBuilder(env, rules, evaluatorCache).build(desc);
      evaluatorCache = updatedCache;
      eval = updatedCache.get(desc);
      if (eval == null) {
        throw new IllegalStateException(
            "updated cache missing evaluator for descriptor - should not happen");
      }
    }
    return eval;
  }

  private static class DescriptorCacheBuilder {
    private final RuleResolver resolver = new RuleResolver();
    private final Env env;
    private final RuleCache ruleCache;
    private final HashMap<Descriptor, MessageEvaluator> cache;

    private DescriptorCacheBuilder(
        Env env, RuleCache ruleCache, Map<Descriptor, MessageEvaluator> previousCache) {
      this.env = Objects.requireNonNull(env, "env");
      this.ruleCache = Objects.requireNonNull(ruleCache, "ruleCache");
      this.cache = new HashMap<>(previousCache);
    }

    /**
     * Creates an immutable cache containing the descriptor (and any other descriptors it
     * references).
     *
     * @param descriptor Descriptor used to build the cache.
     * @return Unmodifiable map of descriptors to evaluators.
     * @throws CompilationException If an error occurs compiling a rule on the cache.
     */
    public Map<Descriptor, MessageEvaluator> build(Descriptor descriptor)
        throws CompilationException {
      createMessageEvaluator(descriptor);
      return Collections.unmodifiableMap(cache);
    }

    private MessageEvaluator createMessageEvaluator(Descriptor desc) throws CompilationException {
      System.err.println("all day");
      MessageEvaluator eval = cache.get(desc);
      if (eval != null) {
        return eval;
      }
      MessageEvaluator msgEval = new MessageEvaluator();
      cache.put(desc, msgEval);
      buildMessage(desc, msgEval);
      return msgEval;
    }

    private void buildMessage(Descriptor desc, MessageEvaluator msgEval)
        throws CompilationException {
      try {
        DynamicMessage defaultInstance = DynamicMessage.newBuilder(desc).buildPartial();
        Descriptor descriptor = defaultInstance.getDescriptorForType();
        MessageRules msgRules = resolver.resolveMessageRules(descriptor);
        if (msgRules.getDisabled()) {
          return;
        }
        System.err.println("process mess ex");
        processMessageExpressions(descriptor, msgRules, msgEval, defaultInstance);
        System.err.println("process mess ex1");
        processOneofRules(descriptor, msgEval);
        System.err.println("process mess ex2");
        processFields(descriptor, msgEval);
        System.err.println("process mess ex DONE");
      } catch (InvalidProtocolBufferException e) {
        throw new CompilationException(
            "failed to parse proto definition: " + desc.getFullName(), e);
      }
    }

    private void processMessageExpressions(
        Descriptor desc, MessageRules msgRules, MessageEvaluator msgEval, DynamicMessage message)
        throws CompilationException {
      List<Rule> celList = msgRules.getCelList();
      if (celList.isEmpty()) {
        return;
      }
      System.err.println("Compiled ass presidency1");
      Env finalEnv =
          env.extend(
              EnvOption.types(message),
              EnvOption.declarations(
                  Decls.newVar(Variable.THIS_NAME, Decls.newObjectType(desc.getFullName()))));
      System.err.println("Compiled ass presidency");
      List<CompiledProgram> compiledPrograms = compileRules(celList, finalEnv, false);
      System.err.println("Bangarang");
      if (compiledPrograms.isEmpty()) {
        throw new CompilationException("compile returned null");
      }
      msgEval.append(new CelPrograms(null, compiledPrograms));
    }

    private void processOneofRules(Descriptor desc, MessageEvaluator msgEval)
        throws InvalidProtocolBufferException, CompilationException {
      List<Descriptors.OneofDescriptor> oneofs = desc.getOneofs();
      for (Descriptors.OneofDescriptor oneofDesc : oneofs) {
        OneofRules oneofRules = resolver.resolveOneofRules(oneofDesc);
        OneofEvaluator oneofEvaluatorEval = new OneofEvaluator(oneofDesc, oneofRules.getRequired());
        msgEval.append(oneofEvaluatorEval);
      }
    }

    private void processFields(Descriptor desc, MessageEvaluator msgEval)
        throws CompilationException, InvalidProtocolBufferException {
      List<FieldDescriptor> fields = desc.getFields();
      for (FieldDescriptor fieldDescriptor : fields) {
        FieldDescriptor descriptor = desc.findFieldByName(fieldDescriptor.getName());
        FieldRules fieldRules = resolver.resolveFieldRules(descriptor);
        System.err.println("build field");
        FieldEvaluator fldEval = buildField(descriptor, fieldRules);
        System.err.println("build field DONE");
        msgEval.append(fldEval);
      }
    }

    private FieldEvaluator buildField(FieldDescriptor fieldDescriptor, FieldRules fieldRules)
        throws CompilationException {
      ValueEvaluator valueEvaluatorEval = new ValueEvaluator(fieldDescriptor, null);
      boolean ignoreDefault = fieldDescriptor.hasPresence() && shouldIgnoreDefault(fieldRules);
      Object zero = null;
      if (ignoreDefault) {
        zero = zeroValue(fieldDescriptor, false);
      }
      FieldEvaluator fieldEvaluator =
          new FieldEvaluator(
              valueEvaluatorEval,
              fieldDescriptor,
              fieldRules.getRequired(),
              fieldDescriptor.hasPresence(),
              fieldRules.getIgnore(),
              zero);
      buildValue(fieldDescriptor, fieldRules, fieldEvaluator.valueEvaluator);
      return fieldEvaluator;
    }

    private static boolean shouldIgnoreEmpty(FieldRules rules) {
      return rules.getIgnore() == Ignore.IGNORE_IF_UNPOPULATED
          || rules.getIgnore() == Ignore.IGNORE_IF_DEFAULT_VALUE;
    }

    private static boolean shouldIgnoreDefault(FieldRules rules) {
      return rules.getIgnore() == Ignore.IGNORE_IF_DEFAULT_VALUE;
    }

    private void buildValue(
        FieldDescriptor fieldDescriptor, FieldRules fieldRules, ValueEvaluator valueEvaluator)
        throws CompilationException {
      if (fieldRules.getIgnore() == Ignore.IGNORE_ALWAYS) {
        return;
      }

      processIgnoreEmpty(fieldDescriptor, fieldRules, valueEvaluator);
      processFieldExpressions(fieldDescriptor, fieldRules, valueEvaluator);
      processEmbeddedMessage(fieldDescriptor, valueEvaluator);
      processWrapperRules(fieldDescriptor, fieldRules, valueEvaluator);
      processStandardRules(fieldDescriptor, fieldRules, valueEvaluator);
      processAnyRules(fieldDescriptor, fieldRules, valueEvaluator);
      processEnumRules(fieldDescriptor, fieldRules, valueEvaluator);
      processMapRules(fieldDescriptor, fieldRules, valueEvaluator);
      processRepeatedRules(fieldDescriptor, fieldRules, valueEvaluator);
    }

    private void processIgnoreEmpty(
        FieldDescriptor fieldDescriptor, FieldRules fieldRules, ValueEvaluator valueEvaluatorEval)
        throws CompilationException {
      if (valueEvaluatorEval.hasNestedRule() && shouldIgnoreEmpty(fieldRules)) {
        valueEvaluatorEval.setIgnoreEmpty(zeroValue(fieldDescriptor, true));
      }
    }

    private Object zeroValue(FieldDescriptor fieldDescriptor, boolean forItems)
        throws CompilationException {
      final Object zero;
      if (forItems && fieldDescriptor.isRepeated()) {
        switch (fieldDescriptor.getType().getJavaType()) {
          case INT:
            zero = 0;
            break;
          case LONG:
            zero = 0L;
            break;
          case FLOAT:
            zero = 0F;
            break;
          case DOUBLE:
            zero = 0D;
            break;
          case BOOLEAN:
            zero = false;
            break;
          case STRING:
            zero = "";
            break;
          case BYTE_STRING:
            zero = ByteString.EMPTY;
            break;
          case ENUM:
            zero = fieldDescriptor.getEnumType().getValues().get(0);
            break;
          case MESSAGE:
            zero = createMessageForType(fieldDescriptor.getMessageType());
            break;
          default:
            zero = fieldDescriptor.getDefaultValue();
            break;
        }
      } else if (fieldDescriptor.getJavaType() == FieldDescriptor.JavaType.MESSAGE
          && !fieldDescriptor.isRepeated()) {
        zero = createMessageForType(fieldDescriptor.getMessageType());
      } else {
        zero = fieldDescriptor.getDefaultValue();
      }
      return zero;
    }

    private Message createMessageForType(Descriptor messageType) throws CompilationException {
      try {
        return DynamicMessage.parseFrom(messageType, new byte[0]);
      } catch (InvalidProtocolBufferException e) {
        throw new CompilationException("field descriptor type is invalid " + e.getMessage(), e);
      }
    }

    private void processFieldExpressions(
        FieldDescriptor fieldDescriptor, FieldRules fieldRules, ValueEvaluator valueEvaluatorEval)
        throws CompilationException {
      List<Rule> rulesCelList = fieldRules.getCelList();
      if (rulesCelList.isEmpty()) {
        return;
      }

      Decl celType =
          Decls.newVar(
              Variable.THIS_NAME,
              DescriptorMappings.getCELType(fieldDescriptor, valueEvaluatorEval.hasNestedRule()));

      List<EnvOption> opts = Arrays.asList(EnvOption.declarations(celType));
      if (fieldDescriptor.getJavaType() == FieldDescriptor.JavaType.MESSAGE) {
        try {
          DynamicMessage defaultInstance =
              DynamicMessage.parseFrom(fieldDescriptor.getMessageType(), new byte[0]);
          opts.add(EnvOption.types(defaultInstance));
        } catch (InvalidProtocolBufferException e) {
          throw new CompilationException("field descriptor type is invalid " + e.getMessage(), e);
        }
      }

      Env finalEnv = env.extend(opts.toArray(new EnvOption[opts.size()]));
      List<CompiledProgram> compiledPrograms = compileRules(rulesCelList, finalEnv, true);
      if (!compiledPrograms.isEmpty()) {
        valueEvaluatorEval.append(new CelPrograms(valueEvaluatorEval, compiledPrograms));
      }
    }

    private void processEmbeddedMessage(
        FieldDescriptor fieldDescriptor, ValueEvaluator valueEvaluatorEval)
        throws CompilationException {
      if (fieldDescriptor.getJavaType() != FieldDescriptor.JavaType.MESSAGE
          || fieldDescriptor.isMapField()
          || (fieldDescriptor.isRepeated() && !valueEvaluatorEval.hasNestedRule())) {
        return;
      }
      Evaluator embedEval =
          new EmbeddedMessageEvaluator(
              valueEvaluatorEval, createMessageEvaluator(fieldDescriptor.getMessageType()));
      valueEvaluatorEval.append(embedEval);
    }

    private void processWrapperRules(
        FieldDescriptor fieldDescriptor, FieldRules fieldRules, ValueEvaluator valueEvaluatorEval)
        throws CompilationException {
      if (fieldDescriptor.getJavaType() != FieldDescriptor.JavaType.MESSAGE
          || fieldDescriptor.isMapField()
          || (fieldDescriptor.isRepeated() && !valueEvaluatorEval.hasNestedRule())) {
        return;
      }
      FieldDescriptor expectedWrapperDescriptor =
          DescriptorMappings.expectedWrapperRules(fieldDescriptor.getMessageType().getFullName());
      if (expectedWrapperDescriptor == null || !fieldRules.hasField(expectedWrapperDescriptor)) {
        return;
      }
      ValueEvaluator unwrapped =
          new ValueEvaluator(
              valueEvaluatorEval.getDescriptor(), valueEvaluatorEval.getNestedRule());
      buildValue(fieldDescriptor.getMessageType().findFieldByName("value"), fieldRules, unwrapped);
      valueEvaluatorEval.append(unwrapped);
    }

    private void processStandardRules(
        FieldDescriptor fieldDescriptor, FieldRules fieldRules, ValueEvaluator valueEvaluatorEval)
        throws CompilationException {
      List<CompiledProgram> compile =
          ruleCache.compile(fieldDescriptor, fieldRules, valueEvaluatorEval.hasNestedRule());
      if (compile.isEmpty()) {
        return;
      }
      valueEvaluatorEval.append(new CelPrograms(valueEvaluatorEval, compile));
    }

    private void processAnyRules(
        FieldDescriptor fieldDescriptor, FieldRules fieldRules, ValueEvaluator valueEvaluatorEval) {
      if ((fieldDescriptor.isRepeated() && !valueEvaluatorEval.hasNestedRule())
          || fieldDescriptor.getJavaType() != FieldDescriptor.JavaType.MESSAGE
          || !fieldDescriptor.getMessageType().getFullName().equals("google.protobuf.Any")) {
        return;
      }
      FieldDescriptor typeURLDesc = fieldDescriptor.getMessageType().findFieldByName("type_url");
      valueEvaluatorEval.append(
          new AnyEvaluator(
              valueEvaluatorEval,
              typeURLDesc,
              fieldRules.getAny().getInList(),
              fieldRules.getAny().getNotInList()));
    }

    private void processEnumRules(
        FieldDescriptor fieldDescriptor, FieldRules fieldRules, ValueEvaluator valueEvaluatorEval) {
      if (fieldDescriptor.getJavaType() != FieldDescriptor.JavaType.ENUM) {
        return;
      }
      if (fieldRules.getEnum().getDefinedOnly()) {
        Descriptors.EnumDescriptor enumDescriptor = fieldDescriptor.getEnumType();
        valueEvaluatorEval.append(
            new EnumEvaluator(valueEvaluatorEval, enumDescriptor.getValues()));
      }
    }

    private void processMapRules(
        FieldDescriptor fieldDescriptor, FieldRules fieldRules, ValueEvaluator valueEvaluatorEval)
        throws CompilationException {
      if (!fieldDescriptor.isMapField()) {
        return;
      }
      MapEvaluator mapEval = new MapEvaluator(valueEvaluatorEval, fieldDescriptor);
      buildValue(
          fieldDescriptor.getMessageType().findFieldByNumber(1),
          fieldRules.getMap().getKeys(),
          mapEval.getKeyEvaluator());
      buildValue(
          fieldDescriptor.getMessageType().findFieldByNumber(2),
          fieldRules.getMap().getValues(),
          mapEval.getValueEvaluator());
      valueEvaluatorEval.append(mapEval);
    }

    private void processRepeatedRules(
        FieldDescriptor fieldDescriptor, FieldRules fieldRules, ValueEvaluator valueEvaluatorEval)
        throws CompilationException {
      if (fieldDescriptor.isMapField()
          || !fieldDescriptor.isRepeated()
          || valueEvaluatorEval.hasNestedRule()) {
        return;
      }
      ListEvaluator listEval = new ListEvaluator(valueEvaluatorEval);
      buildValue(fieldDescriptor, fieldRules.getRepeated().getItems(), listEval.itemRules);
      valueEvaluatorEval.append(listEval);
    }

    private static List<CompiledProgram> compileRules(List<Rule> rules, Env env, boolean isField)
        throws CompilationException {
      List<Expression> expressions = Expression.fromRules(rules);
      List<CompiledProgram> compiledPrograms = new ArrayList<>();
      for (int i = 0; i < expressions.size(); i++) {
        Expression expression = expressions.get(i);
        AstExpression astExpression = AstExpression.newAstExpression(env, expression);
        @Nullable FieldPath rulePath = null;
        if (isField) {
          rulePath =
              FieldPath.newBuilder()
                  .addElements(CEL_FIELD_PATH_ELEMENT.toBuilder().setIndex(i))
                  .build();
        }
        compiledPrograms.add(
            new CompiledProgram(
                env.program(astExpression.ast),
                astExpression.source,
                rulePath,
                new MessageValue(rules.get(i))));
      }
      return compiledPrograms;
    }
  }
}
