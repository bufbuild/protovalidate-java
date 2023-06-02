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

package build.buf.protovalidate.Evaluator;

import build.buf.protovalidate.Constraints.Cache;
import build.buf.protovalidate.Expression.ProgramSet;
import build.buf.validate.Constraint;
import build.buf.validate.FieldConstraints;
import build.buf.validate.MessageConstraints;
import build.buf.validate.OneofConstraints;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;
import dev.cel.runtime.CelRuntimeBuilder;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static build.buf.protovalidate.Expression.Compiler.compile;

public class Builder {
    // TODO: (TCN-1708) based on benchmarks, about 50% of CPU time is spent obtaining a read
    //  lock on this mutex. Ideally, this can be reworked to be thread-safe while
    //  minimizing the need to obtain a lock.
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<Descriptor, MessageEvaluator> cache = new HashMap<>();
    private final CelRuntimeBuilder env;
    private final Cache constraints;
    private final ConstraintResolver resolver;
    private Loader load;

    public Builder(CelRuntimeBuilder env, boolean disableLazy, ConstraintResolver res, List<Descriptor> seedDesc) {
        this.env = env;
        this.constraints = new Cache();
        this.resolver = res;

        if (disableLazy) {
            this.load = this::load;
        } else {
            this.load = this::loadOrBuild;
        }

        for (Descriptor desc : seedDesc) {
            build(desc);
        }
    }


    /**
     * @param desc descriptor of the message to load
     * @return the evaluator for the message
     * load returns a pre-cached MessageEvaluator for the given descriptor or, if
     * the descriptor is unknown, returns an evaluator that always resolves to an
     * errors.CompilationError.
     */
    public MessageEvaluator load(Descriptor desc) {
        MessageEvaluator evaluator = cache.get(desc);
        if (evaluator == null) {
            return new UnknownMessage(desc);
        }

        return evaluator;
    }

    private MessageEvaluator loadOrBuild(Descriptor desc) {
        lock.readLock().lock();
        try {
            MessageEvaluator eval = cache.get(desc);
            if (eval != null) {
                return eval;
            }
        } finally {
            lock.readLock().unlock();
        }

        lock.writeLock().lock();
        try {
            return build(desc);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private MessageEvaluator build(Descriptor desc) {
        MessageEvaluator eval = cache.get(desc);
        if (eval != null) {
            return eval;
        }

        MessageEvaluatorImpl msgEval = new MessageEvaluatorImpl(null);
        cache.put(desc, msgEval);

        buildMessage(desc, msgEval);
        return msgEval;
    }

    private void buildMessage(Descriptor desc, MessageEvaluatorImpl msgEval) {
        MessageConstraints msgConstraints = resolver.resolveMessageConstraints(desc);
        if (msgConstraints.getDisabled()) {
            return;
        }

        List<Processor> steps = Arrays.asList(
                this::processMessageExpressions,
                this::processOneofConstraints,
                this::processFields);

        for (Processor step : steps) {
            try {
                step.process(desc, msgConstraints, msgEval);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void processMessageExpressions(Descriptor desc, MessageConstraints msgConstraints, MessageEvaluatorImpl msgEval) {
        try {
            env.addMessageTypes(desc);
            // TODO: "this" not assigned
            // cel.variable("this", cel.objectType(String.valueOf(desc.fullName())
            env.setTypeFactory(descriptor -> DynamicMessage.newBuilder(desc));
            ProgramSet compiledExpression = compile(msgConstraints.getCelList(), env);
            msgEval.append(new CelPrograms(compiledExpression));
        } catch (Exception e) {
            msgEval.setErr(e);
        }
    }

    private void processOneofConstraints(Descriptor desc, MessageConstraints msgConstraints, MessageEvaluatorImpl msgEval) {
        List<Descriptors.OneofDescriptor> oneofs = desc.getOneofs();
        for (int i = 0; i < oneofs.size(); i++) {
            Descriptors.OneofDescriptor oneofDesc = oneofs.get(i);
            OneofConstraints oneofConstraints = resolver.resolveOneofConstraints(oneofDesc);
            Oneof oneofEval = new Oneof(oneofDesc, oneofConstraints.getRequired());
            msgEval.append(oneofEval);
        }
    }

    private void processFields(Descriptor desc, MessageConstraints msgConstraints, MessageEvaluatorImpl msgEval) {
        List<FieldDescriptor> fields = desc.getFields();
        for (int i = 0; i < fields.size(); i++) {
            Descriptors.FieldDescriptor fdesc = fields.get(i);
            FieldConstraints fieldConstraints = resolver.resolveFieldConstraints(fdesc);
            FieldEval fldEval;
            try {
                fldEval = buildField(fdesc, fieldConstraints);
            } catch (Exception e) {
                msgEval.setErr(e);
                return;
            }
            msgEval.append(fldEval);
        }
    }

    private FieldEval buildField(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints) throws Exception {
        FieldEval fieldEval = new FieldEval(
                new Value(null, false),
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
            try {
                step.process(fieldDescriptor, fieldConstraints, forItems, valueEval);
            } catch (Exception e) {
                throw e;
            }
        }
    }

    public void processZeroValue(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, Boolean forItems, Value valueEval) throws Exception {
        valueEval.setZero((Message) fieldDescriptor.getDefaultValue());
        if (forItems && fieldDescriptor.isRepeated()) {
            DynamicMessage.Builder msgBuilder = DynamicMessage.newBuilder(fieldDescriptor.getContainingType());
            DynamicMessage msg = msgBuilder.build();
            // val.Zero = msg.Get(fdesc).List().NewElement()
            Message field = (Message) msg.getField(fieldDescriptor);
//            valueEval.setZero();
        }
    }

    public void processFieldExpressions(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, Boolean forItems, Value valueEval) throws Exception {
        List<Constraint> constraints = fieldConstraints.getCelList();
        if (constraints.isEmpty()) {
            return;
        }

        if (fieldDescriptor.getType() == FieldDescriptor.Type.MESSAGE) {
            env.addFileTypes(fieldDescriptor.getFile());
            // TODO: "this" not assigned
            // constraints.Variable("this", constraints.ObjectType(String.valueOf(fieldDescriptor.getMessageType().getFullName())))
            env.setTypeFactory(descriptor -> DynamicMessage.newBuilder(fieldConstraints));
        } else {
            // TODO: "this" not assigned
            // ProtoKindToCELType(fieldDescriptor.getType())
        }

        ProgramSet compiledExpressions;
        try {
            compiledExpressions = compile(constraints, env);
        } catch (Exception e) {
            throw e;
        }

//        if (!compiledExpressions.isEmpty()) {
//            valueEval.getConstraints().add(new CelPrograms(compiledExpressions));
//        }
    }

    public void processEmbeddedMessage(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, Boolean forItems, Value valueEval) throws Exception {
        if (fieldDescriptor.getType() != FieldDescriptor.Type.MESSAGE ||
                fieldConstraints.getSkipped() ||
                fieldDescriptor.isMapField() || (fieldDescriptor.isRepeated() && !forItems)) {
            return;
        }

        MessageEvaluatorImpl embedEval = (MessageEvaluatorImpl) this.build(fieldDescriptor.getMessageType());
        Exception err = embedEval.getErr();
        if (err != null) {
            throw err;
        }
        valueEval.append(embedEval);

    }

    public void processWrapperConstraints(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, Boolean forItems, Value valueEval) throws Exception {
        if (fieldDescriptor.getType() != FieldDescriptor.Type.MESSAGE ||
                fieldConstraints.getSkipped() ||
                fieldDescriptor.isMapField() || (fieldDescriptor.isRepeated() && !forItems)) {
            return;
        }

    }

    public void processStandardConstraints(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, Boolean forItems, Value valueEval) throws Exception {
    }

    public void processAnyConstraints(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, Boolean forItems, Value valueEval) throws Exception {
    }

    public void processEnumConstraints(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, Boolean forItems, Value valueEval) throws Exception {
    }

    public void processMapConstraints(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, Boolean forItems, Value valueEval) throws Exception {
    }

    public void processRepeatedConstraints(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, Boolean forItems, Value valueEval) throws Exception {
    }

    // Each step in 'steps' list above is a FieldProcessor
    @FunctionalInterface
    private interface FieldProcessor {
        void process(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, Boolean forItems, Value valueEval) throws Exception;
    }

    // Each step in 'steps' list above is a Processor
    @FunctionalInterface
    private interface Processor {
        void process(Descriptor desc, MessageConstraints msgConstraints, MessageEvaluatorImpl msgEval) throws Exception;
    }

    @FunctionalInterface
    private interface Loader {
        MessageEvaluator load(Descriptor desc);
    }

    public class LoaderImpl implements Loader {
        @Override
        public MessageEvaluator load(Descriptor desc) {
            return null;
        }
    }
}