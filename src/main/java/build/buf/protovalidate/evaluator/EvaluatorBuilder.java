// Copyright 2023 Buf Technologies, Inc.
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

package build.buf.protovalidate.evaluator;

import build.buf.protovalidate.constraints.Cache;
import build.buf.protovalidate.constraints.Lookups;
import build.buf.protovalidate.results.CompilationException;
import build.buf.protovalidate.expression.Compiler;
import build.buf.protovalidate.expression.ProgramSet;
import build.buf.validate.Constraint;
import build.buf.validate.FieldConstraints;
import build.buf.validate.MessageConstraints;
import build.buf.validate.OneofConstraints;
import build.buf.validate.ValidateProto;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.InvalidProtocolBufferException;
import org.projectnessie.cel.Env;
import org.projectnessie.cel.EnvOption;
import org.projectnessie.cel.checker.Decls;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EvaluatorBuilder {
    // TODO: apparently go has some concurrency issues?

    private final Map<Descriptor, MessageEvaluator> cache = new HashMap<>();
    private final Env env;
    private final Cache constraints;
    private final ConstraintResolver resolver;
    private final ExtensionRegistry extensionRegistry = ExtensionRegistry.newInstance();
    private final boolean disableLazy;

    public EvaluatorBuilder(Env env, boolean disableLazy, ConstraintResolver res) {
        this.env = env;
        this.constraints = new Cache();
        this.resolver = res;
        this.disableLazy = disableLazy;
        extensionRegistry.add(ValidateProto.message);
        extensionRegistry.add(ValidateProto.field);
        extensionRegistry.add(ValidateProto.oneof);
    }

    public MessageEvaluator load(Descriptor desc) throws CompilationException {
        if (disableLazy) {
            return loadDescriptor(desc);
        } else {
            return loadOrBuildDescriptor(desc);
        }
    }

    private MessageEvaluator build(Descriptor desc) throws CompilationException {
        MessageEvaluator eval = cache.get(desc);
        if (eval != null) {
            return eval;
        }
        MessageEvaluator msgEval = new MessageEvaluatorImpl();
        cache.put(desc, msgEval);
        buildMessage(desc, msgEval);
        return msgEval;
    }

    private void buildMessage(Descriptor desc, MessageEvaluator msgEval) throws CompilationException {
        try {
            DynamicMessage defaultInstance = DynamicMessage.parseFrom(desc, new byte[0], extensionRegistry);
            Descriptor descriptor = defaultInstance.getDescriptorForType();
            MessageConstraints msgConstraints = resolver.resolveMessageConstraints(descriptor);
            if (msgConstraints.getDisabled()) {
                return;
            }
            processMessageExpressions(descriptor, msgConstraints, msgEval, defaultInstance);
            processOneofConstraints(descriptor, msgEval);
            processFields(descriptor, msgEval);
        } catch (InvalidProtocolBufferException e) {
            throw new CompilationException("failed to parse proto definition: " + desc.getFullName());
        }
    }

    private void processMessageExpressions(Descriptor desc, MessageConstraints msgConstraints, MessageEvaluator msgEval, DynamicMessage message) throws CompilationException {
        List<Constraint> celList = msgConstraints.getCelList();
        if (celList.isEmpty()) {
            return;
        }
        ProgramSet compiledExpressions = Compiler.compileConstraints(
                celList,
                env,
                EnvOption.types(message),
                EnvOption.declarations(
                        Decls.newVar("this", Decls.newObjectType(desc.getFullName()))
                )
        );
        if (compiledExpressions.programs.isEmpty()) {
            throw new CompilationException("compile returned null");
        }
        msgEval.append(new CelPrograms(compiledExpressions));
    }

    private void processOneofConstraints(Descriptor desc, MessageEvaluator msgEval) {
        List<Descriptors.OneofDescriptor> oneofs = desc.getOneofs();
        for (Descriptors.OneofDescriptor oneofDesc : oneofs) {
            OneofConstraints oneofConstraints = resolver.resolveOneofConstraints(oneofDesc);
            OneofEvaluator oneofEvaluatorEval = new OneofEvaluator(oneofDesc, oneofConstraints.getRequired());
            msgEval.append(oneofEvaluatorEval);
        }
    }

    private void processFields(Descriptor desc, MessageEvaluator msgEval) throws CompilationException {
        List<FieldDescriptor> fields = desc.getFields();
        for (FieldDescriptor fieldDescriptor : fields) {
            FieldDescriptor descriptor = desc.findFieldByName(fieldDescriptor.getName());
            FieldConstraints fieldConstraints = resolver.resolveFieldConstraints(descriptor);
            FieldEvaluator fldEval = buildField(descriptor, fieldConstraints);
            msgEval.append(fldEval);
        }
    }

    private FieldEvaluator buildField(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints) throws CompilationException {
        ValueEvaluator valueEvaluatorEval = new ValueEvaluator();
        valueEvaluatorEval.ignoreEmpty = fieldConstraints.getIgnoreEmpty();
        FieldEvaluator fieldEvaluator = new FieldEvaluator(
                valueEvaluatorEval,
                fieldDescriptor,
                fieldConstraints.getRequired(),
                fieldDescriptor.hasPresence()
        );

        buildValue(
                fieldDescriptor,
                fieldConstraints,
                false,
                fieldEvaluator.valueEvaluator
        );
        return fieldEvaluator;
    }


    private void buildValue(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, boolean forItems, ValueEvaluator valueEvaluatorEval) throws CompilationException {
        valueEvaluatorEval.ignoreEmpty = fieldConstraints.getIgnoreEmpty();
        processZeroValue(fieldDescriptor, forItems, valueEvaluatorEval);
        processFieldExpressions(fieldDescriptor, fieldConstraints, valueEvaluatorEval);
        processEmbeddedMessage(fieldDescriptor, fieldConstraints, forItems, valueEvaluatorEval);
        processWrapperConstraints(fieldDescriptor, fieldConstraints, forItems, valueEvaluatorEval);
        processStandardConstraints(fieldDescriptor, fieldConstraints, forItems, valueEvaluatorEval);
        processAnyConstraints(fieldDescriptor, fieldConstraints, forItems, valueEvaluatorEval);
        processEnumConstraints(fieldDescriptor, fieldConstraints, valueEvaluatorEval);
        processMapConstraints(fieldDescriptor, fieldConstraints, valueEvaluatorEval);
        processRepeatedConstraints(fieldDescriptor, fieldConstraints, forItems, valueEvaluatorEval);
    }

    // TODO: this seems off
    private void processZeroValue(FieldDescriptor fieldDescriptor, Boolean forItems, ValueEvaluator valueEvaluatorEval) {
        if (fieldDescriptor.getType() == FieldDescriptor.Type.MESSAGE) {
            valueEvaluatorEval.zero = DynamicMessage.getDefaultInstance(fieldDescriptor.getContainingType());
        } else {
            valueEvaluatorEval.zero = fieldDescriptor.getDefaultValue();
        }
        if (forItems && fieldDescriptor.isRepeated()) {
            DynamicMessage msg = DynamicMessage.getDefaultInstance(fieldDescriptor.getContainingType());
            valueEvaluatorEval.zero = msg.getField(fieldDescriptor);
        }
    }

    private void processFieldExpressions(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, ValueEvaluator valueEvaluatorEval) throws CompilationException {
        List<Constraint> constraintsCelList = fieldConstraints.getCelList();
        if (constraintsCelList.isEmpty()) {
            return;
        }
        List<EnvOption> opts;
        if (fieldDescriptor.getType() == FieldDescriptor.Type.MESSAGE) {
            try {
                DynamicMessage defaultInstance = DynamicMessage.parseFrom(fieldDescriptor.getMessageType(), new byte[0], extensionRegistry);
                opts = Arrays.asList(
                        EnvOption.types(defaultInstance),
                        EnvOption.declarations(Decls.newVar("this", Decls.newObjectType(fieldDescriptor.getMessageType().getFullName())))
                );
            } catch (InvalidProtocolBufferException e) {
                throw new CompilationException("field descriptor type is invalid " + e.getMessage());
            }
        } else {
            opts = Collections.singletonList(
                    EnvOption.declarations(Decls.newVar("this", Lookups.protoKindToCELType(fieldDescriptor.getType())))
            );
        }
        ProgramSet compiledExpressions = Compiler.compileConstraints(constraintsCelList, env, opts.toArray(new EnvOption[0]));
        if (!compiledExpressions.isEmpty()) {
            valueEvaluatorEval.append(new CelPrograms(compiledExpressions));
        }
    }

    private void processEmbeddedMessage(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, Boolean forItems, ValueEvaluator valueEvaluatorEval) throws CompilationException {
        if (fieldDescriptor.getType() != FieldDescriptor.Type.MESSAGE ||
                fieldConstraints.getSkipped() ||
                fieldDescriptor.isMapField() || (fieldDescriptor.isRepeated() && !forItems)) {
            return;
        }

        MessageEvaluator embedEval = this.build(fieldDescriptor.getMessageType());
        valueEvaluatorEval.append(embedEval);
    }

    private void processWrapperConstraints(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, Boolean forItems, ValueEvaluator valueEvaluatorEval) throws CompilationException {
        if (fieldDescriptor.getType() != FieldDescriptor.Type.MESSAGE ||
                fieldConstraints.getSkipped() ||
                fieldDescriptor.isMapField() || (fieldDescriptor.isRepeated() && !forItems)) {
            return;
        }
        FieldDescriptor expectedWrapperDescriptor = Lookups.expectedWrapperConstraints(fieldDescriptor.getMessageType().getFullName());
        if (expectedWrapperDescriptor == null || !fieldConstraints.hasField(expectedWrapperDescriptor)) {
            return;
        }

        ValueEvaluator unwrapped = new ValueEvaluator();
        buildValue(
                fieldDescriptor.getMessageType().findFieldByName("value"),
                fieldConstraints,
                true,
                unwrapped);

        valueEvaluatorEval.append(unwrapped);
    }

    private void processStandardConstraints(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, Boolean forItems, ValueEvaluator valueEvaluatorEval) throws CompilationException {
        ProgramSet stdConstraints = constraints.build(env, fieldDescriptor, fieldConstraints, forItems);
        // TODO: verify null check error handling, it may not need to be handled when there are no constraints
        if (stdConstraints == null) {
            return;
        }
        CelPrograms eval = new CelPrograms(stdConstraints);
        valueEvaluatorEval.append(eval);
    }

    private void processAnyConstraints(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, Boolean forItems, ValueEvaluator valueEvaluatorEval) {
        if ((fieldDescriptor.isRepeated() && !forItems) ||
                fieldDescriptor.getType() != FieldDescriptor.Type.MESSAGE ||
                !fieldDescriptor.getMessageType().getFullName().equals("google.protobuf.Any")) {
            return;
        }

        FieldDescriptor typeURLDesc = fieldDescriptor.getMessageType().findFieldByName("type_url");
        AnyEvaluator anyEvaluatorEval = new AnyEvaluator(typeURLDesc,
                fieldConstraints.getAny().getInList().toArray(new String[0]),
                fieldConstraints.getAny().getNotInList().toArray(new String[0]));

        valueEvaluatorEval.append(anyEvaluatorEval);
    }

    private void processEnumConstraints(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, ValueEvaluator valueEvaluatorEval) {
        if (fieldDescriptor.getJavaType() != FieldDescriptor.JavaType.ENUM) {
            return;
        }
        if (fieldConstraints.getEnum().getDefinedOnly()) {
            Descriptors.EnumDescriptor enumDescriptor = fieldDescriptor.getEnumType();
            Descriptors.EnumValueDescriptor[] values = enumDescriptor.getValues().toArray(new Descriptors.EnumValueDescriptor[0]);
            valueEvaluatorEval.append(new EnumEvaluator(values));
        }
    }

    private void processMapConstraints(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, ValueEvaluator valueEvaluatorEval) throws CompilationException {
        if (!fieldDescriptor.isMapField()) {
            return;
        }

        MapEvaluator mapEval = new MapEvaluator();
        buildValue(
                fieldDescriptor.getMessageType().findFieldByNumber(1),
                fieldConstraints.getMap().getKeys(),
                true,
                mapEval.keyConstraints);
        buildValue(
                fieldDescriptor.getMessageType().findFieldByNumber(2),
                fieldConstraints.getMap().getValues(),
                true,
                mapEval.valueEvaluatorConstraints);
        valueEvaluatorEval.append(mapEval);
    }

    private void processRepeatedConstraints(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, Boolean forItems, ValueEvaluator valueEvaluatorEval) throws CompilationException {
        if (fieldDescriptor.isMapField() || !fieldDescriptor.isRepeated() || forItems) {
            return;
        }

        ListEvaluator listEval = new ListEvaluator();
        buildValue(fieldDescriptor, fieldConstraints.getRepeated().getItems(), true, listEval.itemConstraints);
        valueEvaluatorEval.append(listEval);
    }

    /**
     * @param descriptor descriptor of the message to load
     * @return the evaluator for the message
     * load returns a pre-cached MessageEvaluator for the given descriptor or, if
     * the descriptor is unknown, returns an evaluator that always resolves to an
     * errors.CompilationError.
     */
    private MessageEvaluator loadDescriptor(Descriptor descriptor) {
        MessageEvaluator evaluator = cache.get(descriptor);
        if (evaluator == null) {
            return new UnknownMessageEvaluator(descriptor);
        }
        return evaluator;
    }

    private MessageEvaluator loadOrBuildDescriptor(Descriptor descriptor) throws CompilationException {
        MessageEvaluator eval = cache.get(descriptor);
        if (eval != null) {
            return eval;
        }
        return build(descriptor);
    }
}
