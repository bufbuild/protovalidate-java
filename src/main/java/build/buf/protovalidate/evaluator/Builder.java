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
import build.buf.protovalidate.errors.CompilationError;
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

public class Builder {
    // TODO: apparently go has some concurrency issues?

    private final Map<Descriptor, MessageEvaluator> cache = new HashMap<>();
    private final Env env;
    private final Cache constraints;
    private final ConstraintResolver resolver;
    private final Loader loader;
    private final ExtensionRegistry extensionRegistry = ExtensionRegistry.newInstance();

    public Builder(Env env, boolean disableLazy, ConstraintResolver res, List<Descriptor> seedDesc) {
        this.env = env;
        this.constraints = new Cache();
        this.resolver = res;
        if (disableLazy) {
            this.loader = this::load;
        } else {
            this.loader = this::loadOrBuild;
        }

        for (Descriptor desc : seedDesc) {
            try {
                this.loader.load(desc);
            } catch (CompilationError e) {
                throw new RuntimeException(e);
            }
        }
        extensionRegistry.add(ValidateProto.message);
        extensionRegistry.add(ValidateProto.field);
        extensionRegistry.add(ValidateProto.oneof);
    }

    /**
     * @param desc descriptor of the message to load
     * @return the evaluator for the message
     * load returns a pre-cached MessageEvaluator for the given descriptor or, if
     * the descriptor is unknown, returns an evaluator that always resolves to an
     * errors.CompilationError.
     */
    private MessageEvaluator load(Descriptor desc) {
        MessageEvaluator evaluator = cache.get(desc);
        if (evaluator == null) {
            return new UnknownMessage(desc);
        }
        return evaluator;
    }

    public MessageEvaluator loadOrBuild(Descriptor desc) throws CompilationError {
        MessageEvaluator eval = cache.get(desc);
        if (eval != null) {
            return eval;
        }
        return build(desc);
    }

    private MessageEvaluator build(Descriptor desc) throws CompilationError {
        MessageEvaluator eval = cache.get(desc);
        if (eval != null) {
            return eval;
        }
        MessageEvaluator msgEval = new MessageEvaluatorImpl();
        cache.put(desc, msgEval);
        buildMessage(desc, msgEval);
        return msgEval;
    }

    private void buildMessage(Descriptor desc, MessageEvaluator msgEval) throws CompilationError {
        try {
            DynamicMessage defaultInstance = DynamicMessage.parseFrom(desc, new byte[0], extensionRegistry);
            Descriptor descriptor = defaultInstance.getDescriptorForType();
            MessageConstraints msgConstraints = resolver.resolveMessageConstraints(descriptor);
            if (msgConstraints.getDisabled()) {
                return;
            }
            processMessageExpressions(descriptor, msgConstraints, msgEval, defaultInstance);
            processOneofConstraints(descriptor, msgConstraints, msgEval);
            processFields(descriptor, msgEval);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }

    private void processMessageExpressions(Descriptor desc, MessageConstraints msgConstraints, MessageEvaluator msgEval, DynamicMessage message) throws CompilationError {
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
        if (compiledExpressions == null) {
            throw new RuntimeException("compile returned null");
        }
        msgEval.append(new CelPrograms(compiledExpressions));
    }

    private void processOneofConstraints(Descriptor desc, MessageConstraints msgConstraints, MessageEvaluator msgEval) {
        List<Descriptors.OneofDescriptor> oneofs = desc.getOneofs();
        for (Descriptors.OneofDescriptor oneofDesc : oneofs) {
            OneofConstraints oneofConstraints = resolver.resolveOneofConstraints(oneofDesc);
            Oneof oneofEval = new Oneof(oneofDesc, oneofConstraints.getRequired());
            msgEval.append(oneofEval);
        }
    }

    private void processFields(Descriptor desc, MessageEvaluator msgEval) throws CompilationError {
        List<FieldDescriptor> fields = desc.getFields();
        for (FieldDescriptor fieldDescriptor : fields) {
            FieldDescriptor descriptor = desc.findFieldByName(fieldDescriptor.getName());
            FieldConstraints fieldConstraints = resolver.resolveFieldConstraints(descriptor);
            FieldEval fldEval = buildField(descriptor, fieldConstraints);
            msgEval.append(fldEval);
        }
    }

    private FieldEval buildField(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints) throws CompilationError {
        Value valueEval = new Value(fieldConstraints.getIgnoreEmpty());
        FieldEval fieldEval = new FieldEval(
                valueEval,
                fieldDescriptor,
                fieldConstraints.getRequired(),
                fieldDescriptor.hasPresence()
        );

        buildValue(
                fieldDescriptor,
                fieldConstraints,
                false,
                fieldEval.value
        );
        return fieldEval;
    }


    private void buildValue(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, boolean forItems, Value valueEval) throws CompilationError {
        valueEval.ignoreEmpty = fieldConstraints.getIgnoreEmpty();
        processZeroValue(fieldDescriptor, fieldConstraints, forItems, valueEval);
        processFieldExpressions(fieldDescriptor, fieldConstraints, forItems, valueEval);
        processEmbeddedMessage(fieldDescriptor, fieldConstraints, forItems, valueEval);
        processWrapperConstraints(fieldDescriptor, fieldConstraints, forItems, valueEval);
        processStandardConstraints(fieldDescriptor, fieldConstraints, forItems, valueEval);
        processAnyConstraints(fieldDescriptor, fieldConstraints, forItems, valueEval);
        processEnumConstraints(fieldDescriptor, fieldConstraints, forItems, valueEval);
        processMapConstraints(fieldDescriptor, fieldConstraints, forItems, valueEval);
        processRepeatedConstraints(fieldDescriptor, fieldConstraints, forItems, valueEval);
    }

    private void processZeroValue(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, Boolean forItems, Value valueEval) {
        if (fieldDescriptor.getType() == FieldDescriptor.Type.MESSAGE) {
            valueEval.zero = DynamicMessage.getDefaultInstance(fieldDescriptor.getContainingType());
        } else {
            valueEval.zero = fieldDescriptor.getDefaultValue();
        }
        if (forItems && fieldDescriptor.isRepeated()) {
            DynamicMessage msg = DynamicMessage.getDefaultInstance(fieldDescriptor.getContainingType());
            valueEval.zero = msg.getField(fieldDescriptor);
        }
    }

    private void processFieldExpressions(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, Boolean forItems, Value valueEval) throws CompilationError {
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
                throw CompilationError.newCompilationError("field descriptor type is invalid", e);
            }
        } else {
            opts = Collections.singletonList(
                    EnvOption.declarations(Decls.newVar("this", Lookups.protoKindToCELType(fieldDescriptor.getType())))
            );
        }
        ProgramSet compiledExpressions = Compiler.compileConstraints(constraintsCelList, env, opts.toArray(new EnvOption[0]));
        if (!compiledExpressions.isEmpty()) {
            valueEval.constraints.append(new CelPrograms(compiledExpressions));
        }
    }

    private void processEmbeddedMessage(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, Boolean forItems, Value valueEval) throws CompilationError {
        if (fieldDescriptor.getType() != FieldDescriptor.Type.MESSAGE ||
                fieldConstraints.getSkipped() ||
                fieldDescriptor.isMapField() || (fieldDescriptor.isRepeated() && !forItems)) {
            return;
        }

        MessageEvaluator embedEval = this.build(fieldDescriptor.getMessageType());
        valueEval.append(embedEval);
    }

    private void processWrapperConstraints(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, Boolean forItems, Value valueEval) throws CompilationError {
        if (fieldDescriptor.getType() != FieldDescriptor.Type.MESSAGE ||
                fieldConstraints.getSkipped() ||
                fieldDescriptor.isMapField() || (fieldDescriptor.isRepeated() && !forItems)) {
            return;
        }
        FieldDescriptor expectedWrapperDescriptor = Lookups.expectedWrapperConstraints(fieldDescriptor.getMessageType().getFullName());
        if (expectedWrapperDescriptor == null || !fieldConstraints.hasField(expectedWrapperDescriptor)) {
            return;
        }

        Value unwrapped = new Value();
        buildValue(
                fieldDescriptor.getMessageType().findFieldByName("value"),
                fieldConstraints,
                true,
                unwrapped);

        valueEval.append(unwrapped.constraints);
    }

    private void processStandardConstraints(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, Boolean forItems, Value valueEval) throws CompilationError {
        ProgramSet stdConstraints = constraints.build(env, fieldDescriptor, fieldConstraints, forItems);
        // TODO: verify null check error handling, it may not need to be handled when there are no constraints
        if (stdConstraints == null) {
            return;
        }
        CelPrograms eval = new CelPrograms(stdConstraints);
        valueEval.append(eval);
    }

    private void processAnyConstraints(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, Boolean forItems, Value valueEval) throws CompilationError {
        if ((fieldDescriptor.isRepeated() && !forItems) ||
                fieldDescriptor.getType() != FieldDescriptor.Type.MESSAGE ||
                !fieldDescriptor.getMessageType().getFullName().equals("google.protobuf.Any")) {
            return;
        }

        FieldDescriptor typeURLDesc = fieldDescriptor.getMessageType().findFieldByName("type_url");
        Any anyEval = new Any(typeURLDesc,
                fieldConstraints.getAny().getInList().toArray(new String[0]),
                fieldConstraints.getAny().getNotInList().toArray(new String[0]));

        valueEval.append(anyEval);
    }

    private void processEnumConstraints(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, Boolean forItems, Value valueEval) throws CompilationError {
        if (fieldDescriptor.getJavaType() != FieldDescriptor.JavaType.ENUM) {
            return;
        }
        if (fieldConstraints.getEnum().getDefinedOnly()) {
            Descriptors.EnumDescriptor enumDescriptor = fieldDescriptor.getEnumType();
            Descriptors.EnumValueDescriptor[] values = enumDescriptor.getValues().toArray(new Descriptors.EnumValueDescriptor[0]);
            valueEval.append(new DefinedEnum(values));
        }
    }

    private void processMapConstraints(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, Boolean forItems, Value valueEval) throws CompilationError {
        if (!fieldDescriptor.isMapField()) {
            return;
        }

        KvPairs mapEval = new KvPairs();
        buildValue(
                fieldDescriptor.getMessageType().findFieldByNumber(1),
                fieldConstraints.getMap().getKeys(),
                true,
                mapEval.keyConstraints);
        buildValue(
                fieldDescriptor.getMessageType().findFieldByNumber(2),
                fieldConstraints.getMap().getValues(),
                true,
                mapEval.valueConstraints);
        valueEval.append(mapEval);
    }

    private void processRepeatedConstraints(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, Boolean forItems, Value valueEval) throws CompilationError {
        if (!fieldDescriptor.isRepeated() || forItems) {
            return;
        }

        ListItems listEval = new ListItems();
        buildValue(fieldDescriptor, fieldConstraints.getRepeated().getItems(), true, listEval.itemConstraints);
        valueEval.append(listEval);
    }

    public Loader getLoader() {
        return loader;
    }

    @FunctionalInterface
    public interface Loader {
        MessageEvaluator load(Descriptor desc) throws CompilationError;
    }
}