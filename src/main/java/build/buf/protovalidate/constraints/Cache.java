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

import build.buf.protovalidate.expression.*;
import build.buf.validate.Constraint;
import build.buf.validate.FieldConstraints;
import build.buf.validate.ValidateProto;
import com.google.api.expr.v1alpha1.Type;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import org.projectnessie.cel.Ast;
import org.projectnessie.cel.Env;
import org.projectnessie.cel.EnvOption;
import org.projectnessie.cel.ProgramOption;
import org.projectnessie.cel.checker.Decls;
import org.projectnessie.cel.interpreter.ResolvedValue;
import org.projectnessie.cel.tools.ScriptCreateException;

import java.util.*;

public class Cache {
    private final Map<FieldDescriptor, AstSet> cache;

    public Cache() {
        this.cache = new HashMap<>();
    }

    private Message resolveConstraints(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, Boolean forItems) {
        boolean ok = true;
        FieldDescriptor oneofFieldDescriptor = fieldConstraints.getOneofFieldDescriptor(Lookups.FIELD_CONSTRAINTS_ONEOF_DESC);
        if (oneofFieldDescriptor == null) {
            // TODO: throw exception? or just return null?
            return null;
        }
        FieldDescriptor expectedConstraintDescriptor = getExpectedConstraintDescriptor(fieldDescriptor, forItems);
        if (expectedConstraintDescriptor == null) {
            // TODO: throw exception? or just return null?
            ok = false;
        }

        if (ok && !oneofFieldDescriptor.getFullName().equals(expectedConstraintDescriptor.getFullName())) {
            // TODO: throw exception
        }

        if (!ok || !fieldConstraints.hasField(oneofFieldDescriptor)) {
            // TODO: work out what to do here
            return null;
        }
        return (Message) fieldConstraints.getField(oneofFieldDescriptor);
    }

    private Env prepareEnvironment(Env env, FieldDescriptor fieldDesc, Message rules, Boolean forItems) {
        env.extend(
                EnvOption.types(rules.getDefaultInstanceForType()),
                EnvOption.declarations(
                        Decls.newVar("this", getCELType(fieldDesc, forItems)),
                        Decls.newVar("rules", Decls.newObjectType(rules.getDescriptorForType().getFullName()))
                )
        );
        return env;
    }

    private AstSet loadOrCompileStandardConstraint(Env env, FieldDescriptor constraintFieldDesc) {
        if (cache.get(constraintFieldDesc) != null) {
            return cache.get(constraintFieldDesc);
        }
        FieldConstraints constraints = constraintFieldDesc.getOptions().getExtension(ValidateProto.field);
        List<CompiledAst> asts = new ArrayList<>();
        for (Constraint constraint : constraints.getCelList()) {
            Env.AstIssuesTuple astIssuesTuple = env.parse(constraint.getExpression());
            if (astIssuesTuple.hasIssues()) {
                throw new RuntimeException(new ScriptCreateException("unable to parse constraint for ast.", astIssuesTuple.getIssues()));
            }
            Ast ast = astIssuesTuple.getAst();
            Env.AstIssuesTuple check = env.check(ast);
            if (check.hasIssues()) {
                throw new RuntimeException(new ScriptCreateException("unable to parse constraint for ast.", check.getIssues()));
            }
            asts.add(new CompiledAst(ast, new Expression(constraint)));
        }
        AstSet astSet = new AstSet(env, asts);
        cache.put(constraintFieldDesc, astSet);
        return astSet;
    }

    private FieldDescriptor getExpectedConstraintDescriptor(FieldDescriptor fieldDescriptor, Boolean forItems) {
        if (fieldDescriptor.isMapField()) {
            return Lookups.MAP_FIELD_CONSTRAINTS_DESC;
        } else if (fieldDescriptor.isRepeated() && !forItems) {
            return Lookups.REPEATED_FIELD_CONSTRAINTS_DESC;
        } else if (fieldDescriptor.getJavaType() == FieldDescriptor.JavaType.MESSAGE) {
            Message message = (Message) fieldDescriptor.getDefaultValue();
            return Lookups.EXPECTED_WKT_CONSTRAINTS.get(message.getDescriptorForType().getFullName());
        } else {
            return Lookups.EXPECTED_STANDARD_CONSTRAINTS.get(fieldDescriptor.getType());
        }
    }

    private Type getCELType(FieldDescriptor fieldDescriptor, Boolean forItems) {
        if (!forItems) {
            if (fieldDescriptor.isMapField()) {
                return Decls.newMapType(
                        getCELType(fieldDescriptor.getMessageType().findFieldByNumber(1), true),
                        getCELType(fieldDescriptor.getMessageType().findFieldByNumber(2), true)
                );
            } else if (fieldDescriptor.isRepeated()) {
                // TODO: find correct return type
                return null;
            }
        }

        if (fieldDescriptor.getType() == FieldDescriptor.Type.MESSAGE) {
            String fqn = fieldDescriptor.getMessageType().getFullName();
            switch (fqn) {
                case "google.protobuf.Any":
                    return Decls.newWellKnownType(Type.WellKnownType.ANY);
                case "google.protobuf.Duration":
                    return Decls.newWellKnownType(Type.WellKnownType.DURATION);
                case "google.protobuf.Timestamp":
                    return Decls.newWellKnownType(Type.WellKnownType.TIMESTAMP);
                default:
                    return Decls.newObjectType(fieldDescriptor.getFullName());
            }
        }

        return Lookups.protoKindToCELType(fieldDescriptor.getType());
    }

    public ProgramSet build(Env env, FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, Boolean forItems) {
        Message message = resolveConstraints(fieldDescriptor, fieldConstraints, forItems);
        if (message == null) {
            // TODO: there's a doneness check from go but we'll ignore it for now.
            return null;
        }
        Env prepareEnvironment = prepareEnvironment(env, fieldDescriptor, message, forItems);
        if (prepareEnvironment == null) {
            // TODO: go actually has this fail sometimes.
            return null;
        }
        AstSet completeSet = new AstSet(env, Collections.emptyList());
        for (FieldDescriptor field : message.getDescriptorForType().getFields()) {
            AstSet precomputedAst = loadOrCompileStandardConstraint(env, field);
            completeSet.merge(precomputedAst);
        }
        Variable rules = new Variable("rules", message.getDefaultInstanceForType());
        return completeSet.reduceResiduals(ProgramOption.globals(rules));
    }
}