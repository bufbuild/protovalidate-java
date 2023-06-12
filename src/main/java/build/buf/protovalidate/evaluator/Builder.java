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
import org.projectnessie.cel.common.types.TimestampT;

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

    public Builder(Env env, boolean disableLazy, ConstraintResolver res, List<Descriptor> seedDesc) {
        this.env = env;
        this.constraints = new Cache();
        this.resolver = res;
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
        MessageEvaluatorImpl msgEval = new MessageEvaluatorImpl();
        cache.put(desc, msgEval);
        MessageConstraints msgConstraints = resolver.resolveMessageConstraints(desc);
        if (msgConstraints.getDisabled()) {
            return msgEval;
        }
        processMessageExpressions(desc, msgConstraints, msgEval);
        processOneofConstraints(desc, msgConstraints, msgEval);
        processFields(desc, msgConstraints, msgEval);
        return msgEval;
    }

    private void processMessageExpressions(Descriptor desc, MessageConstraints msgConstraints, MessageEvaluator msgEval) throws CompilationError {
        List<Constraint> celList = msgConstraints.getCelList();
        if (celList.isEmpty()) {
            return;
        }
        try {
            ExtensionRegistry extensionRegistry = ExtensionRegistry.newInstance();
            extensionRegistry.add(ValidateProto.message);
            extensionRegistry.add(ValidateProto.field);
            extensionRegistry.add(ValidateProto.oneof);
            DynamicMessage defaultInstance = DynamicMessage.parseFrom(desc, new byte[0], extensionRegistry);
            ProgramSet compiledExpressions = Compiler.compile(
                    celList,
                    env,
                    EnvOption.types(defaultInstance),
                    EnvOption.declarations(
                            Decls.newVar("this", Decls.newObjectType(desc.getFullName())),
                            Decls.newVar("now", Decls.newObjectType(TimestampT.TimestampType.typeName()))
                    )
            );
            if (compiledExpressions == null) {
                throw new RuntimeException("compile returned null");
            }
            msgEval.append(new CelPrograms(compiledExpressions));
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }

    private void processOneofConstraints(Descriptor desc, MessageConstraints msgConstraints, MessageEvaluator msgEval) {
        List<Descriptors.OneofDescriptor> oneofs = desc.getOneofs();
        for (Descriptors.OneofDescriptor oneofDesc : oneofs) {
            OneofConstraints oneofConstraints = resolver.resolveOneofConstraints(oneofDesc);
            Oneof oneofEval = new Oneof(oneofDesc, oneofConstraints.getRequired());
            msgEval.append(oneofEval);
        }
    }

    private void processFields(Descriptor desc, MessageConstraints msgConstraints, MessageEvaluator msgEval) {
        List<FieldDescriptor> fields = desc.getFields();
        for (FieldDescriptor fdesc : fields) {
            FieldConstraints fieldConstraints = resolver.resolveFieldConstraints(fdesc);
            FieldEval fldEval;
            try {
                fldEval = buildField(fdesc, fieldConstraints);
            } catch (Exception e) {
                return;
            }
            msgEval.append(fldEval);
        }
    }

    private FieldEval buildField(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints) throws Exception {
        FieldEval fieldEval = new FieldEval(
                fieldDescriptor,
                fieldConstraints.getRequired(),
                fieldDescriptor.hasPresence()
        );
        try {
            buildValue(
                    fieldDescriptor,
                    fieldConstraints,
                    false,
                    fieldEval.getValue()
            );
        } catch (Exception e) {
            throw e;
        }
        return fieldEval;
    }


    private void buildValue(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, boolean forItems, Value valueEval) throws Exception {
        valueEval.setIgnoreEmpty(fieldConstraints.getIgnoreEmpty());
        List<FieldProcessor> steps = Arrays.asList(
                this::processZeroValue,
                this::processFieldExpressions,
                this::processEmbeddedMessage,
                this::processWrapperConstraints,
                this::processStandardConstraints,
                this::processAnyConstraints,
                this::processEnumConstraints,
                this::processMapConstraints,
                this::processRepeatedConstraints
        );

        for (FieldProcessor step : steps) {
            step.process(fieldDescriptor, fieldConstraints, forItems, valueEval);
        }
    }

    private void processZeroValue(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, Boolean forItems, Value valueEval) throws Exception {
        // TODO: implement me!
    }

    private void processFieldExpressions(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, Boolean forItems, Value valueEval) throws Exception {
        List<Constraint> exprs = fieldConstraints.getCelList();
        if (exprs.isEmpty()) {
            return;
        }

        List<EnvOption> opts;
        if (fieldDescriptor.getType() == FieldDescriptor.Type.MESSAGE) {
            opts = Arrays.asList(
                    EnvOption.types(fieldDescriptor.getFile().toProto()),
                    EnvOption.declarations(Decls.newVar("this", Decls.newObjectType(fieldDescriptor.getMessageType().getFullName())))
            );
        } else {
            opts = Collections.singletonList(
                    EnvOption.declarations(Decls.newVar("this", Lookups.protoKindToCELType(fieldDescriptor.getType())))
            );
        }

        ProgramSet compiledExpressions = Compiler.compile(exprs, env, opts.toArray(new EnvOption[0]));
        if (!compiledExpressions.isEmpty()) {
            valueEval.getConstraints().append(new CelPrograms(compiledExpressions));
        }
    }

    private void processEmbeddedMessage(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, Boolean forItems, Value valueEval) throws Exception {
        if (fieldDescriptor.getType() != FieldDescriptor.Type.MESSAGE ||
                fieldConstraints.getSkipped() ||
                fieldDescriptor.isMapField() || (fieldDescriptor.isRepeated() && !forItems)) {
            return;
        }

        MessageEvaluator embedEval = this.build(fieldDescriptor.getMessageType());
        valueEval.append(embedEval);
    }

    private void processWrapperConstraints(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, Boolean forItems, Value valueEval) throws Exception {
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
        try {
            buildValue(
                    fieldDescriptor.getMessageType().findFieldByName("value"),
                    fieldConstraints,
                    true,
                    unwrapped);
        } catch (Exception e) {
            return;
        }

        valueEval.append(unwrapped.getConstraints());
    }

    private void processStandardConstraints(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, Boolean forItems, Value valueEval) throws Exception {
        ProgramSet stdConstraints = constraints.build(env, fieldDescriptor, fieldConstraints, forItems);
        valueEval.append(new CelPrograms(stdConstraints));
    }

    private void processAnyConstraints(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, Boolean forItems, Value valueEval) throws Exception {
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

    private void processEnumConstraints(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, Boolean forItems, Value valueEval) throws Exception {
        // TODO: implement me!
        if (fieldDescriptor.getJavaType() != FieldDescriptor.JavaType.ENUM) {
            return;
        }
        if (fieldConstraints.getEnum().getDefinedOnly()) {
            Descriptors.EnumDescriptor enumDescriptor = fieldDescriptor.getEnumType();
            Descriptors.EnumValueDescriptor[] values = enumDescriptor.getValues().toArray(new Descriptors.EnumValueDescriptor[0]);
            valueEval.append(new DefinedEnum(values));
        }
    }

    private void processMapConstraints(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, Boolean forItems, Value valueEval) throws Exception {
        if (!fieldDescriptor.isMapField()) {
            return;
        }

        KvPairs mapEval = new KvPairs();
        try {
            buildValue(
                    fieldDescriptor.getMessageType().findFieldByNumber(1),
                    fieldConstraints.getMap().getKeys(),
                    true,
                    mapEval.getKeyConstraints()
            );
        } catch (Exception e) {
            // TODO: something with the exception
            return;
        }

        try {
            buildValue(
                    fieldDescriptor.getMessageType().findFieldByNumber(2),
                    fieldConstraints,
                    false,
                    mapEval.getValueConstraints()
            );
        } catch (Exception e) {
            // TODO: something with the exception
            return;
        }

        valueEval.append(mapEval);
    }

    private void processRepeatedConstraints(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, Boolean forItems, Value valueEval) throws Exception {
        if (!fieldDescriptor.isRepeated() || forItems) {
            return;
        }

        ListItems listEval = new ListItems();
        try {
            buildValue(fieldDescriptor, fieldConstraints.getRepeated().getItems(), true, listEval.getItemConstraints());
        } catch (Exception e) {
            // TODO: something with the exception
            return;
        }

        valueEval.append(listEval);
    }

    // Each step in 'steps' list above is a FieldProcessor
    @FunctionalInterface
    private interface FieldProcessor {
        void process(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, Boolean forItems, Value valueEval) throws Exception;
    }

    // Each step in 'steps' list above is a Processor
    @FunctionalInterface
    private interface Processor {
        void process(Descriptor desc, MessageConstraints msgConstraints, MessageEvaluator msgEval) throws Exception;
    }

    @FunctionalInterface
    private interface Loader {
        MessageEvaluator load(Descriptor desc);
    }

    private class LoaderImpl implements Loader {
        @Override
        public MessageEvaluator load(Descriptor desc) {
            return null;
        }
    }
}