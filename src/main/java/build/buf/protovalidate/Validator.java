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
import dev.cel.runtime.CelRuntime;
import dev.cel.runtime.CelRuntimeBuilder;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class Validator {
    private final Builder builder;
    private final boolean failFast;

    public Validator(ValidatorOption... options) throws Exception {
        Config cfg = new Config();
        for (ValidatorOption option : options) {
            option.apply(cfg);
        }

        CelRuntimeBuilder env = CelExt.defaultCelRuntime(cfg.useUTC);

        this.builder = new Builder(env, cfg.disableLazy, cfg.resolver, cfg.desc);
        this.failFast = cfg.failFast;
    }

    public void validate(MessageOrBuilder msg) throws Exception {
        if (msg == null) {
            return;
        }

        Descriptor descriptor = msg.getDescriptorForType();
        MessageEvaluator evaluator = builder.load(descriptor);
        evaluator.evaluateMessage(msg, failFast);
    }

    private static class Config {
        private boolean failFast = false;
        private boolean useUTC = false;
        private boolean disableLazy = false;
        private List<Descriptor> desc = new ArrayList<>();
        private StandardConstraintResolver resolver = new DefaultStandardConstraintResolver();

        private void apply(ValidatorOption option) throws Exception {
            option.apply(this);
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

    public interface ValidatorOption {
        void apply(Config config) throws Exception;
    }

    public static ValidatorOption withUTC(boolean useUTC) {
        return config -> config.useUTC = useUTC;
    }

    public static ValidatorOption withFailFast(boolean failFast) {
        return config -> config.failFast = failFast;
    }

    public static ValidatorOption withMessages(Message... messages) {
        return config -> config.desc.addAll(Arrays.asList(getDescriptors(messages)));
    }

    public static ValidatorOption withDescriptors(Descriptor... descriptors) {
        return config -> config.desc.addAll(Arrays.asList(descriptors));
    }

    public static ValidatorOption withDisableLazy(boolean disable) {
        return config -> config.disableLazy = disable;
    }

    private static Descriptor[] getDescriptors(Message... messages) {
        Descriptor[] descriptors = new Descriptor[messages.length];
        for (int i = 0; i < messages.length; i++) {
            descriptors[i] = messages[i].getDescriptorForType();
        }
        return descriptors;
    }
}

