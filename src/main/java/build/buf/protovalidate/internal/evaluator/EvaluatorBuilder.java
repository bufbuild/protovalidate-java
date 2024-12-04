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

package build.buf.protovalidate.internal.evaluator;

import build.buf.protovalidate.Config;
import build.buf.protovalidate.exceptions.CompilationException;
import build.buf.protovalidate.internal.constraints.ConstraintCache;
import build.buf.protovalidate.internal.constraints.DescriptorMappings;
import build.buf.protovalidate.internal.errors.FieldPathUtils;
import build.buf.protovalidate.internal.expression.AstExpression;
import build.buf.protovalidate.internal.expression.CelPrograms;
import build.buf.protovalidate.internal.expression.CompiledProgram;
import build.buf.protovalidate.internal.expression.Expression;
import build.buf.protovalidate.internal.expression.Variable;
import build.buf.validate.Constraint;
import build.buf.validate.FieldConstraints;
import build.buf.validate.FieldPath;
import build.buf.validate.FieldPathElement;
import build.buf.validate.Ignore;
import build.buf.validate.MessageConstraints;
import build.buf.validate.OneofConstraints;
import com.google.common.collect.ImmutableMap;
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
import java.util.Objects;
import javax.annotation.Nullable;
import org.projectnessie.cel.Env;
import org.projectnessie.cel.EnvOption;
import org.projectnessie.cel.checker.Decls;

/** A build-through cache of message evaluators keyed off the provided descriptor. */
public class EvaluatorBuilder {
  private static final FieldPathElement CEL_FIELD_PATH_ELEMENT =
      FieldPathUtils.fieldPathElement(
              FieldConstraints.getDescriptor().findFieldByNumber(FieldConstraints.CEL_FIELD_NUMBER))
          .build();

  private volatile ImmutableMap<Descriptor, Evaluator> evaluatorCache = ImmutableMap.of();

  private final Env env;
  private final boolean disableLazy;
  private final ConstraintCache constraints;

  /**
   * Constructs a new {@link EvaluatorBuilder}.
   *
   * @param env The CEL environment for evaluation.
   * @param config The configuration to use for the evaluation.
   */
  public EvaluatorBuilder(Env env, Config config) {
    this.env = env;
    this.disableLazy = config.isDisableLazy();
    this.constraints = new ConstraintCache(env, config);
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
      ImmutableMap<Descriptor, Evaluator> updatedCache =
          new DescriptorCacheBuilder(env, constraints, evaluatorCache).build(desc);
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
    private final ConstraintResolver resolver = new ConstraintResolver();
    private final Env env;
    private final ConstraintCache constraintCache;
    private final HashMap<Descriptor, Evaluator> cache;

    private DescriptorCacheBuilder(
        Env env,
        ConstraintCache constraintCache,
        ImmutableMap<Descriptor, Evaluator> previousCache) {
      this.env = Objects.requireNonNull(env, "env");
      this.constraintCache = Objects.requireNonNull(constraintCache, "constraintCache");
      this.cache = new HashMap<>(previousCache);
    }

    /**
     * Creates an immutable cache containing the descriptor (and any other descriptors it
     * references).
     *
     * @param descriptor Descriptor used to build the cache.
     * @return Immutable map of descriptors to evaluators.
     * @throws CompilationException If an error occurs compiling a constraint on the cache.
     */
    public ImmutableMap<Descriptor, Evaluator> build(Descriptor descriptor)
        throws CompilationException {
      createMessageEvaluator(descriptor);
      return ImmutableMap.copyOf(cache);
    }

    private Evaluator createMessageEvaluator(Descriptor desc) throws CompilationException {
      Evaluator eval = cache.get(desc);
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
        MessageConstraints msgConstraints = resolver.resolveMessageConstraints(descriptor);
        if (msgConstraints.getDisabled()) {
          return;
        }
        processMessageExpressions(descriptor, msgConstraints, msgEval, defaultInstance);
        processOneofConstraints(descriptor, msgEval);
        processFields(descriptor, msgEval);
      } catch (InvalidProtocolBufferException e) {
        throw new CompilationException(
            "failed to parse proto definition: " + desc.getFullName(), e);
      }
    }

    private void processMessageExpressions(
        Descriptor desc,
        MessageConstraints msgConstraints,
        MessageEvaluator msgEval,
        DynamicMessage message)
        throws CompilationException {
      List<Constraint> celList = msgConstraints.getCelList();
      if (celList.isEmpty()) {
        return;
      }
      Env finalEnv =
          env.extend(
              EnvOption.types(message),
              EnvOption.declarations(
                  Decls.newVar(Variable.THIS_NAME, Decls.newObjectType(desc.getFullName()))));
      List<CompiledProgram> compiledPrograms = compileConstraints(celList, finalEnv, false);
      if (compiledPrograms.isEmpty()) {
        throw new CompilationException("compile returned null");
      }
      msgEval.append(new CelPrograms(compiledPrograms));
    }

    private void processOneofConstraints(Descriptor desc, MessageEvaluator msgEval)
        throws InvalidProtocolBufferException, CompilationException {
      List<Descriptors.OneofDescriptor> oneofs = desc.getOneofs();
      for (Descriptors.OneofDescriptor oneofDesc : oneofs) {
        OneofConstraints oneofConstraints = resolver.resolveOneofConstraints(oneofDesc);
        OneofEvaluator oneofEvaluatorEval =
            new OneofEvaluator(oneofDesc, oneofConstraints.getRequired());
        msgEval.append(oneofEvaluatorEval);
      }
    }

    private void processFields(Descriptor desc, MessageEvaluator msgEval)
        throws CompilationException, InvalidProtocolBufferException {
      List<FieldDescriptor> fields = desc.getFields();
      for (FieldDescriptor fieldDescriptor : fields) {
        FieldDescriptor descriptor = desc.findFieldByName(fieldDescriptor.getName());
        FieldConstraints fieldConstraints = resolver.resolveFieldConstraints(descriptor);
        FieldEvaluator fldEval = buildField(descriptor, fieldConstraints);
        msgEval.append(fldEval);
      }
    }

    private FieldEvaluator buildField(
        FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints)
        throws CompilationException {
      ValueEvaluator valueEvaluatorEval = new ValueEvaluator();
      boolean ignoreDefault =
          fieldDescriptor.hasPresence() && shouldIgnoreDefault(fieldConstraints);
      Object zero = null;
      if (ignoreDefault) {
        zero = zeroValue(fieldDescriptor, false);
      }
      FieldEvaluator fieldEvaluator =
          new FieldEvaluator(
              valueEvaluatorEval,
              fieldDescriptor,
              fieldConstraints.getRequired(),
              fieldDescriptor.hasPresence() || shouldIgnoreEmpty(fieldConstraints),
              fieldDescriptor.hasPresence() && shouldIgnoreDefault(fieldConstraints),
              zero);
      buildValue(fieldDescriptor, fieldConstraints, null, fieldEvaluator.valueEvaluator);
      return fieldEvaluator;
    }

    @SuppressWarnings("deprecation")
    private boolean shouldSkip(FieldConstraints constraints) {
      return constraints.getSkipped() || constraints.getIgnore() == Ignore.IGNORE_ALWAYS;
    }

    @SuppressWarnings("deprecation")
    private static boolean shouldIgnoreEmpty(FieldConstraints constraints) {
      return constraints.getIgnoreEmpty()
          || constraints.getIgnore() == Ignore.IGNORE_IF_UNPOPULATED
          || constraints.getIgnore() == Ignore.IGNORE_IF_DEFAULT_VALUE;
    }

    private static boolean shouldIgnoreDefault(FieldConstraints constraints) {
      return constraints.getIgnore() == Ignore.IGNORE_IF_DEFAULT_VALUE;
    }

    private void buildValue(
        FieldDescriptor fieldDescriptor,
        FieldConstraints fieldConstraints,
        @Nullable EvaluatorWrapper itemsWrapper,
        ValueEvaluator valueEvaluator)
        throws CompilationException {
      processIgnoreEmpty(fieldDescriptor, fieldConstraints, itemsWrapper, valueEvaluator);
      processFieldExpressions(fieldDescriptor, fieldConstraints, valueEvaluator);
      processEmbeddedMessage(fieldDescriptor, fieldConstraints, itemsWrapper, valueEvaluator);
      processWrapperConstraints(fieldDescriptor, fieldConstraints, itemsWrapper, valueEvaluator);
      processStandardConstraints(fieldDescriptor, fieldConstraints, itemsWrapper, valueEvaluator);
      processAnyConstraints(fieldDescriptor, fieldConstraints, itemsWrapper, valueEvaluator);
      processEnumConstraints(fieldDescriptor, fieldConstraints, itemsWrapper, valueEvaluator);
      processMapConstraints(fieldDescriptor, fieldConstraints, itemsWrapper, valueEvaluator);
      processRepeatedConstraints(fieldDescriptor, fieldConstraints, itemsWrapper, valueEvaluator);
    }

    private void processIgnoreEmpty(
        FieldDescriptor fieldDescriptor,
        FieldConstraints fieldConstraints,
        @Nullable EvaluatorWrapper itemsWrapper,
        ValueEvaluator valueEvaluatorEval)
        throws CompilationException {
      if (itemsWrapper != null && shouldIgnoreEmpty(fieldConstraints)) {
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
        FieldDescriptor fieldDescriptor,
        FieldConstraints fieldConstraints,
        ValueEvaluator valueEvaluatorEval)
        throws CompilationException {
      List<Constraint> constraintsCelList = fieldConstraints.getCelList();
      if (constraintsCelList.isEmpty()) {
        return;
      }
      List<EnvOption> opts;
      if (fieldDescriptor.getJavaType() == FieldDescriptor.JavaType.MESSAGE) {
        try {
          DynamicMessage defaultInstance =
              DynamicMessage.parseFrom(fieldDescriptor.getMessageType(), new byte[0]);
          opts =
              Arrays.asList(
                  EnvOption.types(defaultInstance),
                  EnvOption.declarations(
                      Decls.newVar(
                          Variable.THIS_NAME,
                          Decls.newObjectType(fieldDescriptor.getMessageType().getFullName()))));
        } catch (InvalidProtocolBufferException e) {
          throw new CompilationException("field descriptor type is invalid " + e.getMessage(), e);
        }
      } else {
        opts =
            Collections.singletonList(
                EnvOption.declarations(
                    Decls.newVar(
                        Variable.THIS_NAME,
                        DescriptorMappings.protoKindToCELType(fieldDescriptor.getType()))));
      }
      Env finalEnv = env.extend(opts.toArray(new EnvOption[0]));
      List<CompiledProgram> compiledPrograms =
          compileConstraints(constraintsCelList, finalEnv, true);
      if (!compiledPrograms.isEmpty()) {
        valueEvaluatorEval.append(new CelPrograms(compiledPrograms));
      }
    }

    private void processEmbeddedMessage(
        FieldDescriptor fieldDescriptor,
        FieldConstraints fieldConstraints,
        @Nullable EvaluatorWrapper itemsWrapper,
        ValueEvaluator valueEvaluatorEval)
        throws CompilationException {
      if (fieldDescriptor.getJavaType() != FieldDescriptor.JavaType.MESSAGE
          || shouldSkip(fieldConstraints)
          || fieldDescriptor.isMapField()
          || (fieldDescriptor.isRepeated() && itemsWrapper == null)) {
        return;
      }
      Evaluator embedEval = createMessageEvaluator(fieldDescriptor.getMessageType());
      valueEvaluatorEval.append(embedEval);
    }

    private void processWrapperConstraints(
        FieldDescriptor fieldDescriptor,
        FieldConstraints fieldConstraints,
        @Nullable EvaluatorWrapper itemsWrapper,
        ValueEvaluator valueEvaluatorEval)
        throws CompilationException {
      if (fieldDescriptor.getJavaType() != FieldDescriptor.JavaType.MESSAGE
          || shouldSkip(fieldConstraints)
          || fieldDescriptor.isMapField()
          || (fieldDescriptor.isRepeated() && itemsWrapper == null)) {
        return;
      }
      FieldDescriptor expectedWrapperDescriptor =
          DescriptorMappings.expectedWrapperConstraints(
              fieldDescriptor.getMessageType().getFullName());
      if (expectedWrapperDescriptor == null
          || !fieldConstraints.hasField(expectedWrapperDescriptor)) {
        return;
      }
      ValueEvaluator unwrapped = new ValueEvaluator();
      buildValue(
          fieldDescriptor.getMessageType().findFieldByName("value"),
          fieldConstraints,
          itemsWrapper,
          unwrapped);
      valueEvaluatorEval.append(unwrapped);
    }

    private void processStandardConstraints(
        FieldDescriptor fieldDescriptor,
        FieldConstraints fieldConstraints,
        @Nullable EvaluatorWrapper itemsWrapper,
        ValueEvaluator valueEvaluatorEval)
        throws CompilationException {
      List<CompiledProgram> compile =
          constraintCache.compile(fieldDescriptor, fieldConstraints, itemsWrapper != null);
      if (compile.isEmpty()) {
        return;
      }
      appendEvaluator(valueEvaluatorEval, new CelPrograms(compile), itemsWrapper);
    }

    private void processAnyConstraints(
        FieldDescriptor fieldDescriptor,
        FieldConstraints fieldConstraints,
        @Nullable EvaluatorWrapper itemsWrapper,
        ValueEvaluator valueEvaluatorEval) {
      if ((fieldDescriptor.isRepeated() && itemsWrapper == null)
          || fieldDescriptor.getJavaType() != FieldDescriptor.JavaType.MESSAGE
          || !fieldDescriptor.getMessageType().getFullName().equals("google.protobuf.Any")) {
        return;
      }
      FieldDescriptor typeURLDesc = fieldDescriptor.getMessageType().findFieldByName("type_url");
      AnyEvaluator anyEvaluatorEval =
          new AnyEvaluator(
              typeURLDesc,
              fieldConstraints.getAny().getInList(),
              fieldConstraints.getAny().getNotInList());
      appendEvaluator(valueEvaluatorEval, anyEvaluatorEval, itemsWrapper);
    }

    private void processEnumConstraints(
        FieldDescriptor fieldDescriptor,
        FieldConstraints fieldConstraints,
        @Nullable EvaluatorWrapper itemsWrapper,
        ValueEvaluator valueEvaluatorEval) {
      if (fieldDescriptor.getJavaType() != FieldDescriptor.JavaType.ENUM) {
        return;
      }
      if (fieldConstraints.getEnum().getDefinedOnly()) {
        Descriptors.EnumDescriptor enumDescriptor = fieldDescriptor.getEnumType();
        appendEvaluator(
            valueEvaluatorEval, new EnumEvaluator(enumDescriptor.getValues()), itemsWrapper);
      }
    }

    private void processMapConstraints(
        FieldDescriptor fieldDescriptor,
        FieldConstraints fieldConstraints,
        @Nullable EvaluatorWrapper itemsWrapper,
        ValueEvaluator valueEvaluatorEval)
        throws CompilationException {
      if (!fieldDescriptor.isMapField()) {
        return;
      }
      MapEvaluator mapEval = new MapEvaluator(fieldDescriptor);
      buildValue(
          fieldDescriptor.getMessageType().findFieldByNumber(1),
          fieldConstraints.getMap().getKeys(),
          MapEvaluator.KEYS_WRAPPER,
          mapEval.getKeyEvaluator());
      buildValue(
          fieldDescriptor.getMessageType().findFieldByNumber(2),
          fieldConstraints.getMap().getValues(),
          MapEvaluator.VALUES_WRAPPER,
          mapEval.getValueEvaluator());
      appendEvaluator(valueEvaluatorEval, mapEval, itemsWrapper);
    }

    private void processRepeatedConstraints(
        FieldDescriptor fieldDescriptor,
        FieldConstraints fieldConstraints,
        @Nullable EvaluatorWrapper itemsWrapper,
        ValueEvaluator valueEvaluatorEval)
        throws CompilationException {
      if (fieldDescriptor.isMapField() || !fieldDescriptor.isRepeated() || itemsWrapper != null) {
        return;
      }
      ListEvaluator listEval = new ListEvaluator(fieldDescriptor);
      buildValue(
          fieldDescriptor,
          fieldConstraints.getRepeated().getItems(),
          ListEvaluator.ITEMS_WRAPPER,
          listEval.itemConstraints);
      appendEvaluator(valueEvaluatorEval, listEval, itemsWrapper);
    }

    private static List<CompiledProgram> compileConstraints(
        List<Constraint> constraints, Env env, boolean isField) throws CompilationException {
      List<Expression> expressions = Expression.fromConstraints(constraints);
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
                new MessageValue(constraints.get(i))));
      }
      return compiledPrograms;
    }
  }

  private static void appendEvaluator(
      ValueEvaluator valueEvaluatorEval, Evaluator embedEval, @Nullable EvaluatorWrapper wrapper) {
    if (wrapper != null) {
      embedEval = wrapper.wrap(embedEval);
    }
    valueEvaluatorEval.append(embedEval);
  }
}
