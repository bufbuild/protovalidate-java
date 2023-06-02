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

package build.buf.protovalidate;

import build.buf.protovalidate.CelExt.CelExt;
import build.buf.protovalidate.Evaluator.Builder;
import build.buf.protovalidate.Evaluator.MessageEvaluator;
import build.buf.protovalidate.Evaluator.StandardConstraintResolver;
import build.buf.validate.FieldConstraints;
import build.buf.validate.MessageConstraints;
import build.buf.validate.OneofConstraints;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.OneofDescriptor;
import dev.cel.runtime.CelRuntimeBuilder;

import java.util.Collections;
import java.util.List;

public class Validator {
    private final Builder builder;
    private final boolean failFast;

    public Validator(Config config) {
        CelRuntimeBuilder env = CelExt.defaultCelRuntime(config.useUTC);
        this.builder = new Builder(env, config.disableLazy, config.resolver, config.desc);
        this.failFast = config.failFast;
    }

    public void validate(MessageOrBuilder msg) throws Exception {
        if (msg == null) {
            return;
        }

        Descriptor descriptor = msg.getDescriptorForType();
        MessageEvaluator evaluator = builder.load(descriptor);
        evaluator.evaluateMessage(msg, failFast);
    }

    public static class Config {
        private final boolean failFast ;
        private final boolean useUTC ;
        private final boolean disableLazy ;
        private final List<Descriptor> desc;
        private final StandardConstraintResolver resolver;

        public Config(boolean failFast, boolean useUTC, boolean disableLazy, List<Descriptor> desc, StandardConstraintResolver resolver) {
            this.failFast = failFast;
            this.useUTC = useUTC;
            this.disableLazy = disableLazy;
            this.desc = desc;
            this.resolver = resolver;
        }

        public Config() {
            this(false, true, true, Collections.emptyList(), new DefaultStandardConstraintResolver());
        }
    }

    private static class DefaultStandardConstraintResolver implements StandardConstraintResolver {
        @Override
        public MessageConstraints resolveMessageConstraints(Descriptor desc) {
            return MessageConstraints.newBuilder().build();
        }

        @Override
        public OneofConstraints resolveOneofConstraints(OneofDescriptor desc) {
            return OneofConstraints.newBuilder().build();
        }

        @Override
        public FieldConstraints resolveFieldConstraints(FieldDescriptor desc) {
            return FieldConstraints.newBuilder().build();
        }
    }
    private static Descriptor[] getDescriptors(Message... messages) {
        Descriptor[] descriptors = new Descriptor[messages.length];
        for (int i = 0; i < messages.length; i++) {
            descriptors[i] = messages[i].getDescriptorForType();
        }
        return descriptors;
    }
}

