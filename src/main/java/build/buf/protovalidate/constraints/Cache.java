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

import build.buf.protovalidate.expression.AstSet;
import build.buf.protovalidate.expression.CompiledAst;
import build.buf.protovalidate.expression.Expression;
import build.buf.protovalidate.expression.ProgramSet;
import build.buf.protovalidate.expression.Variable;
import build.buf.validate.Constraint;
import build.buf.validate.FieldConstraints;
import build.buf.validate.ValidateProto;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import org.projectnessie.cel.Ast;
import org.projectnessie.cel.Env;
import org.projectnessie.cel.EnvOption;
import org.projectnessie.cel.ProgramOption;
import org.projectnessie.cel.checker.Decls;
import org.projectnessie.cel.common.types.TypeT;
import org.projectnessie.cel.common.types.ref.Type;
import org.projectnessie.cel.tools.ScriptCreateException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Cache {
    private final Map<FieldDescriptor, AstSet> cache;

    public Cache() {
        this.cache = new HashMap<>();
    }

    public ProgramSet buildProgram(Env env, FieldDescriptor fieldDesc, FieldConstraints fieldConstraints, Boolean forItems) {
        Message message = resolveConstraints(fieldDesc, fieldConstraints);
        if (message == null) {
            // TODO: there's a doneness check from go but we'll ignore it for now.
            return null;
        }
        Env prepareEnvironment = prepareEnvironment(env, fieldDesc, message);
        if (prepareEnvironment == null) {
            // TODO: go actually has this fail sometimes.
            return null;
        }
        AstSet completeSet = new AstSet(env, Collections.emptyList());
        for (FieldDescriptor field : message.getDescriptorForType().getFields()) {
            AstSet precomputedAst = loadOrCompileStandardConstraint(env, field);
            completeSet.merge(precomputedAst);
        }
        return completeSet.reduceResiduals(ProgramOption.globals(new Variable("rules", message.getDefaultInstanceForType())));
    }

    private Message resolveConstraints(FieldDescriptor fieldDescriptor, FieldConstraints constraints) {
        FieldDescriptor expectedConstraintDescriptor = getExpectedConstraintDescriptor(fieldDescriptor);
        if (expectedConstraintDescriptor != null) {
            switch (constraints.getTypeCase()) {
                case FLOAT:
                    return constraints.getFloat().getDefaultInstanceForType();
                case DOUBLE:
                    return constraints.getDouble().getDefaultInstanceForType();
                case INT32:
                    return constraints.getInt32().getDefaultInstanceForType();
                case INT64:
                    return constraints.getInt64().getDefaultInstanceForType();
                case UINT32:
                    return constraints.getUint32().getDefaultInstanceForType();
                case UINT64:
                    return constraints.getUint64().getDefaultInstanceForType();
                case SINT32:
                    return constraints.getSint32().getDefaultInstanceForType();
                case SINT64:
                    return constraints.getSint64().getDefaultInstanceForType();
                case FIXED32:
                    return constraints.getFixed32().getDefaultInstanceForType();
                case FIXED64:
                    return constraints.getFixed64().getDefaultInstanceForType();
                case SFIXED32:
                    return constraints.getSfixed32().getDefaultInstanceForType();
                case SFIXED64:
                    return constraints.getSfixed64().getDefaultInstanceForType();
                case BOOL:
                    return constraints.getBool().getDefaultInstanceForType();
                case STRING:
                    return constraints.getString().getDefaultInstanceForType();
                case BYTES:
                    return constraints.getBytes().getDefaultInstanceForType();
                case ENUM:
                    return constraints.getEnum().getDefaultInstanceForType();
                case REPEATED:
                    return constraints.getRepeated().getDefaultInstanceForType();
                case MAP:
                    return constraints.getMap().getDefaultInstanceForType();
                case ANY:
                    return constraints.getAny().getDefaultInstanceForType();
                case DURATION:
                    return constraints.getDuration().getDefaultInstanceForType();
                case TIMESTAMP:
                    return constraints.getTimestamp().getDefaultInstanceForType();
                default:
                    break;
            }
        }
        return null;
    }

    private Env prepareEnvironment(Env env, FieldDescriptor fieldDesc, Message rules) {
        env.extend(
                EnvOption.types(rules.getDefaultInstanceForType()),
                EnvOption.declarations(
                        Decls.newVar("this", Decls.newObjectType(rules.getDescriptorForType().getFullName()))
                )
        );
        return env;
    }

    public AstSet loadOrCompileStandardConstraint(Env env, FieldDescriptor constraintFieldDesc) {
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

    private FieldDescriptor getExpectedConstraintDescriptor(FieldDescriptor targetFieldDesc) {
        if (targetFieldDesc.isMapField()) {
            return Lookups.MAP_FIELD_CONSTRAINTS_DESC;
        } else if (targetFieldDesc.isRepeated()) {
            return Lookups.REPEATED_FIELD_CONSTRAINTS_DESC;
        } else if (targetFieldDesc.getType() == FieldDescriptor.Type.MESSAGE) {
            return Lookups.EXPECTED_WKT_CONSTRAINTS.get(targetFieldDesc.getMessageType().getFullName());
        }
        return Lookups.EXPECTED_STANDARD_CONSTRAINTS.get(targetFieldDesc.getType());
    }

    public Type getCELType(FieldDescriptor fieldDescriptor, Boolean forItems) {
        if (fieldDescriptor.getType() == FieldDescriptor.Type.MESSAGE) {
            String fullName = fieldDescriptor.getMessageType().getFullName();
            switch (fullName) {
                case "google.protobuf.Any":
                case "google.protobuf.Duration":
                case "google.protobuf.Timestamp":
                    // TODO
                    throw new RuntimeException("todo needs to be implemented");
                default:
                    return TypeT.newObjectTypeValue(fullName);
            }
        }
        return null;
    }
}