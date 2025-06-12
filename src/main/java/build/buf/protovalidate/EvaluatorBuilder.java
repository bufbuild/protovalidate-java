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
import build.buf.validate.MessageOneofRule;
import build.buf.validate.MessageRules;
import build.buf.validate.OneofRules;
import build.buf.validate.Rule;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import dev.cel.bundle.Cel;
import dev.cel.bundle.CelBuilder;
import dev.cel.common.types.StructTypeReference;
import dev.cel.runtime.CelEvaluationException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/** A build-through cache of message evaluators keyed off the provided descriptor. */
final class EvaluatorBuilder {
  private static final FieldPathElement CEL_FIELD_PATH_ELEMENT =
      FieldPathUtils.fieldPathElement(
          FieldRules.getDescriptor().findFieldByNumber(FieldRules.CEL_FIELD_NUMBER));

  private volatile Map<Descriptor, MessageEvaluator> evaluatorCache = Collections.emptyMap();

  private final Cel cel;
  private final boolean disableLazy;
  private final RuleCache rules;

  /**
   * Constructs a new {@link EvaluatorBuilder}.
   *
   * @param cel The CEL environment for evaluation.
   * @param config The configuration to use for the evaluation.
   */
  EvaluatorBuilder(Cel cel, Config config) {
    this.cel = cel;
    this.disableLazy = false;
    this.rules = new RuleCache(cel, config);
  }

  /**
   * Constructs a new {@link EvaluatorBuilder}.
   *
   * @param cel The CEL environment for evaluation.
   * @param config The configuration to use for the evaluation.
   */
  EvaluatorBuilder(Cel cel, Config config, List<Descriptor> descriptors, boolean disableLazy)
      throws CompilationException {
    Objects.requireNonNull(descriptors, "descriptors must not be null");
    this.cel = cel;
    this.disableLazy = disableLazy;
    this.rules = new RuleCache(cel, config);

    for (Descriptor descriptor : descriptors) {
      this.build(descriptor);
    }
  }

  /**
   * Returns a pre-cached {@link Evaluator} for the given descriptor or, if the descriptor is
   * unknown, returns an evaluator that always throws a {@link CompilationException}.
   *
   * @param desc Protobuf descriptor type.
   * @return An evaluator for the descriptor type.
   * @throws CompilationException If an evaluator can't be created for the specified descriptor.
   */
  Evaluator load(Descriptor desc) throws CompilationException {
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
          new DescriptorCacheBuilder(cel, rules, evaluatorCache).build(desc);
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
    private final Cel cel;
    private final RuleCache ruleCache;
    private final HashMap<Descriptor, MessageEvaluator> cache;

    private DescriptorCacheBuilder(
        Cel cel, RuleCache ruleCache, Map<Descriptor, MessageEvaluator> previousCache) {
      this.cel = Objects.requireNonNull(cel, "cel");
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
    Map<Descriptor, MessageEvaluator> build(Descriptor descriptor) throws CompilationException {
      createMessageEvaluator(descriptor);
      return Collections.unmodifiableMap(cache);
    }

    private MessageEvaluator createMessageEvaluator(Descriptor desc) throws CompilationException {
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
        processMessageExpressions(descriptor, msgRules, msgEval, defaultInstance);
        processMessageOneofRules(descriptor, msgRules, msgEval);
        processOneofRules(descriptor, msgEval);
        processFields(descriptor, msgEval);
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
      Cel finalCel =
          cel.toCelBuilder()
              .addMessageTypes(message.getDescriptorForType())
              .addVar(Variable.THIS_NAME, StructTypeReference.create(desc.getFullName()))
              .build();
      List<CompiledProgram> compiledPrograms = compileRules(celList, finalCel, false);
      if (compiledPrograms.isEmpty()) {
        throw new CompilationException("compile returned null");
      }
      msgEval.append(new CelPrograms(null, compiledPrograms));
    }

    private void processMessageOneofRules(
        Descriptor desc, MessageRules msgRules, MessageEvaluator msgEval)
        throws CompilationException {
      for (MessageOneofRule rule : msgRules.getOneofList()) {
        if (rule.getFieldsCount() == 0) {
          throw new CompilationException(
              String.format(
                  "at least one field must be specified in oneof rule for the message %s",
                  desc.getFullName()));
        }
        Set<FieldDescriptor> fields = new LinkedHashSet<>(rule.getFieldsCount());
        for (String name : rule.getFieldsList()) {
          FieldDescriptor field = desc.findFieldByName(name);
          if (field == null) {
            throw new CompilationException(
                String.format("field %s not found in %s", name, desc.getFullName()));
          }
          if (!fields.add(field)) {
            throw new CompilationException(
                String.format(
                    "duplicate %s in oneof rule for the message %s", name, desc.getFullName()));
          }
        }
        msgEval.append(new MessageOneofEvaluator(new ArrayList<>(fields), rule.getRequired()));
      }
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
        FieldEvaluator fldEval = buildField(descriptor, fieldRules);
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
          case LONG:
            zero = 0L;
            break;
          case FLOAT:
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
            zero = (long) fieldDescriptor.getEnumType().getValues().get(0).getNumber();
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
        zero =
            ProtoAdapter.scalarToCel(fieldDescriptor.getType(), fieldDescriptor.getDefaultValue());
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
      CelBuilder builder = cel.toCelBuilder();
      builder =
          builder.addVar(
              Variable.THIS_NAME,
              DescriptorMappings.getCELType(fieldDescriptor, valueEvaluatorEval.hasNestedRule()));

      if (fieldDescriptor.getJavaType() == FieldDescriptor.JavaType.MESSAGE) {
        builder = builder.addMessageTypes(fieldDescriptor.getMessageType());
      }
      Cel finalCel = builder.build();
      List<CompiledProgram> compiledPrograms = compileRules(rulesCelList, finalCel, true);
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

      // Verify that the expected wrapper rules for this field are equal to the rules specified on
      // the field
      if (expectedWrapperDescriptor != null) {
        FieldDescriptor oneofFieldDescriptor =
            fieldRules.getOneofFieldDescriptor(DescriptorMappings.FIELD_RULES_ONEOF_DESC);
        // If there are no field rules set, just return
        if (oneofFieldDescriptor == null) {
          return;
        }
        if (!expectedWrapperDescriptor
            .getMessageType()
            .getFullName()
            .equals(oneofFieldDescriptor.getMessageType().getFullName())) {
          throw new CompilationException(
              String.format(
                  "mismatched message rules, %s is not a valid rule for field %s",
                  oneofFieldDescriptor.getName(), fieldDescriptor.getName()));
        }
      }
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

      // If this is a wrapper field, just return. Wrapper fields are handled by
      // processWrapperRules and their unwrapped values are passed through the process gauntlet.
      if (fieldDescriptor.getJavaType() == FieldDescriptor.JavaType.MESSAGE) {
        FieldDescriptor expectedWrapperDescriptor =
            DescriptorMappings.expectedWrapperRules(fieldDescriptor.getMessageType().getFullName());
        if (expectedWrapperDescriptor != null) {
          return;
        }
      }

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

    private static List<CompiledProgram> compileRules(List<Rule> rules, Cel cel, boolean isField)
        throws CompilationException {
      List<Expression> expressions = Expression.fromRules(rules);
      List<CompiledProgram> compiledPrograms = new ArrayList<>();
      for (int i = 0; i < expressions.size(); i++) {
        Expression expression = expressions.get(i);
        AstExpression astExpression = AstExpression.newAstExpression(cel, expression);
        @Nullable FieldPath rulePath = null;
        if (isField) {
          rulePath =
              FieldPath.newBuilder()
                  .addElements(CEL_FIELD_PATH_ELEMENT.toBuilder().setIndex(i))
                  .build();
        }
        try {
          compiledPrograms.add(
              new CompiledProgram(
                  cel.createProgram(astExpression.ast),
                  astExpression.source,
                  rulePath,
                  new MessageValue(rules.get(i)),
                  null));
        } catch (CelEvaluationException e) {
          throw new CompilationException("failed to evaluate rule " + rules.get(i).getId(), e);
        }
      }
      return compiledPrograms;
    }
  }
}
