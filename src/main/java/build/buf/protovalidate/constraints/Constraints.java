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

package build.buf.protovalidate.constraints;

import build.buf.protovalidate.errors.ValidationError;
import build.buf.protovalidate.expression.ProgramSet;
import build.buf.validate.Constraint;
import build.buf.validate.FieldConstraints;
import build.buf.validate.MessageConstraints;
import build.buf.validate.ValidateProto;
import com.google.api.expr.v1alpha1.Type;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.OneofDescriptor;
import com.google.protobuf.Message;
import org.projectnessie.cel.Ast;
import org.projectnessie.cel.Env;
import org.projectnessie.cel.checker.Decls;
import org.projectnessie.cel.tools.Script;
import org.projectnessie.cel.tools.ScriptHost;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Constraints implements ConstraintRules {
    private final ScriptHost scriptHost;
    private final Descriptor descriptor;
    private final List<build.buf.protovalidate.constraints.ConstraintRules> constraints;

    public Constraints(ScriptHost scriptHost, Descriptor descriptor) {
        this.scriptHost = scriptHost;
        this.descriptor = descriptor;
        this.constraints = new ArrayList<>();
        if (descriptor.getOptions().hasExtension(ValidateProto.message)) {
            MessageConstraintRules messageConstraint = new MessageConstraintRules(scriptHost, descriptor.getOptions().getExtension(ValidateProto.message));
            constraints.add(messageConstraint);
        }
    }

    @Override
    public boolean validate(String fieldPath, Message message) {
        for (ConstraintRules constraint : constraints) {
            boolean result = constraint.validate(fieldPath, message);
            if (!result) {
                // not sure what to do here
                return false;
            }
        }
        return true;
    }


    public static class MessageConstraintRules implements ConstraintRules {
        private final ScriptHost scriptHost;
        private final MessageConstraints messageConstraints;

        public MessageConstraintRules(ScriptHost scriptHost, MessageConstraints messageConstraints) {
            this.scriptHost = scriptHost;
            this.messageConstraints = messageConstraints;
        }

        @Override
        public boolean validate(String fieldPath, Message message) {
            HashMap<String, Object> finalActivation = new HashMap<>();
            finalActivation.put("this", message);
            try {
                for (Constraint constraint : messageConstraints.getCelList()) {
                    Script script = scriptHost.buildScript(constraint.getExpression())
                            .withDeclarations(Decls.newVar("this", Decls.newObjectType(message.getDescriptorForType().getFullName())))
                            .withTypes(message.getDefaultInstanceForType())
                            .build();
                    return script.execute(Boolean.class, finalActivation);
                }
            } catch (Exception e) {
                throw new ValidationError();
            }
            return false;
        }
    }

    public ProgramSet Build(Env env, FieldDescriptor fieldDesc, FieldConstraints fieldConstraints, Boolean forItems) {
        // TODO: implement me
        return null;
    }


    // TODO: potentially in the wrong package, consider moving to expression because ProgramSet is defined there (or move ProgramSet to Constraints package)
    public ProgramSet build(FieldDescriptor fieldDesc, Message fieldConstraints, boolean forItems) {
        return null;
    }

    private Env prepareEnvironment(Env env, FieldDescriptor fieldDesc, Message rules, boolean forItems) {

        return null;
    }

    // TODO: potentially in the wrong package, consider moving to expression because AstSet is defined there (or move AstSet to Constraints package)
    private Ast loadOrCompileStandardConstraint(Env env, Message constraintField) {
        return null;
    }
}