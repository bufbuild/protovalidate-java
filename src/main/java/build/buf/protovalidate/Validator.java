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
import build.buf.protovalidate.Constraints.Constraints;
import build.buf.protovalidate.Errors.ValidationError;
import build.buf.protovalidate.Evaluator.Builder;
import build.buf.protovalidate.Evaluator.MessageEvaluator;
import build.buf.protovalidate.Evaluator.ConstraintResolver;
import build.buf.validate.Constraint;
import build.buf.validate.FieldConstraints;
import build.buf.validate.MessageConstraints;
import build.buf.validate.OneofConstraints;
import build.buf.validate.ValidateProto;
import com.google.api.expr.v1alpha1.Decl;
import com.google.api.expr.v1alpha1.Expr;
import com.google.api.expr.v1alpha1.ParsedExpr;
import com.google.api.expr.v1alpha1.Type;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.OneofDescriptor;
import org.projectnessie.cel.Ast;
import org.projectnessie.cel.Env;
import org.projectnessie.cel.Issues;
import org.projectnessie.cel.Program;
import org.projectnessie.cel.checker.Decls;
import org.projectnessie.cel.common.CELError;
import org.projectnessie.cel.interpreter.Activation;
import org.projectnessie.cel.tools.Script;
import org.projectnessie.cel.tools.ScriptCreateException;
import org.projectnessie.cel.tools.ScriptException;
import org.projectnessie.cel.tools.ScriptHost;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.projectnessie.cel.CEL.parsedExprToAst;
import static org.projectnessie.cel.Env.newEnv;
import static org.projectnessie.cel.EnvOption.container;
import static org.projectnessie.cel.EnvOption.declarations;
import static org.projectnessie.cel.EnvOption.types;


public class Validator {
    private final Builder builder;
    private final boolean failFast;


    public Validator(Config config) {
        this.builder = new Builder(config.disableLazy, config.resolver, config.desc);
        this.failFast = config.failFast;
    }

    public void validate(Message msg) {
        ScriptHost scriptHost = ScriptHost.newBuilder().build();
        Constraints constraints = new Constraints(scriptHost, msg.getDescriptorForType());
        constraints.validate("", msg);
    }

    public static class Config {
        private final boolean failFast;
        private final boolean useUTC;
        private final boolean disableLazy;
        private final List<Descriptor> desc;
        private final ConstraintResolver resolver;

        public Config(boolean failFast, boolean useUTC, boolean disableLazy, List<Descriptor> desc, ConstraintResolver resolver) {
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

    private static class DefaultStandardConstraintResolver implements ConstraintResolver {
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

