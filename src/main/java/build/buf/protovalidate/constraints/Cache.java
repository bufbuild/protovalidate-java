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

import build.buf.protovalidate.errors.CompilationError;
import build.buf.protovalidate.expression.AstSet;
import build.buf.protovalidate.expression.Compiler;
import build.buf.protovalidate.expression.ProgramSet;
import build.buf.protovalidate.expression.Variable;
import build.buf.validate.FieldConstraints;
import build.buf.validate.priv.PrivateProto;
import com.google.api.expr.v1alpha1.Type;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import org.projectnessie.cel.Env;
import org.projectnessie.cel.EnvOption;
import org.projectnessie.cel.checker.Decls;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.projectnessie.cel.ProgramOption.globals;

public class Cache {
    private final ConcurrentMap<FieldDescriptor, AstSet> cache;

    public Cache() {
        this.cache = new ConcurrentHashMap<>();
    }

    // This method resolves constraints for a given field based on the provided field descriptor, field constraints, and a flag indicating whether it is for items.
    private Message resolveConstraints(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, Boolean forItems) throws CompilationError {
        // Get the oneof field descriptor from the field constraints.
        FieldDescriptor oneofFieldDescriptor = fieldConstraints.getOneofFieldDescriptor(Lookups.FIELD_CONSTRAINTS_ONEOF_DESC);
        if (oneofFieldDescriptor == null) {
            // If the oneof field descriptor is null there are no constraints to resolve.
            return null;
        }

        // Get the expected constraint descriptor based on the provided field descriptor and the flag indicating whether it is for items.
        FieldDescriptor expectedConstraintDescriptor = getExpectedConstraintDescriptor(fieldDescriptor, forItems);
        boolean ok = expectedConstraintDescriptor != null;
        if (ok && !oneofFieldDescriptor.getFullName().equals(expectedConstraintDescriptor.getFullName())) {
            // If the expected constraint does not match the actual oneof constraint, throw a CompilationError.
            throw CompilationError.newCompilationError("expected constraint %s, got %s on field %s",
                    expectedConstraintDescriptor.getName(),
                    oneofFieldDescriptor.getName(),
                    fieldDescriptor.getName());
        }

        // If the expected constraint descriptor is null or if the field constraints do not have the oneof field descriptor
        // there are no constraints to resolve, so return null.
        if (!ok || !fieldConstraints.hasField(oneofFieldDescriptor)) {
            return null;
        }

        // Return the field from the field constraints identified by the oneof field descriptor, casted as a Message.
        return (Message) fieldConstraints.getField(oneofFieldDescriptor);
    }


    private Env prepareEnvironment(Env env, FieldDescriptor fieldDesc, Message rules, Boolean forItems) {
        return env.extend(
                EnvOption.types(rules.getDefaultInstanceForType()),
                EnvOption.declarations(
                        Decls.newVar("this", getCELType(fieldDesc, forItems)),
                        Decls.newVar("rules", Decls.newObjectType(rules.getDescriptorForType().getFullName()))
                )
        );
    }

    private AstSet loadOrCompileStandardConstraint(Env env, FieldDescriptor constraintFieldDesc) throws CompilationError {
        final AstSet cachedValue = cache.get(constraintFieldDesc);
        if (cachedValue != null) {
            return cachedValue;
        }
        build.buf.validate.priv.FieldConstraints constraints = constraintFieldDesc.getOptions().getExtension(PrivateProto.field);
        AstSet astSet = Compiler.compileASTs(constraints.getCelList(), env);
        cache.put(constraintFieldDesc, astSet);
        return astSet;
    }

    private FieldDescriptor getExpectedConstraintDescriptor(FieldDescriptor fieldDescriptor, Boolean forItems) {
        if (fieldDescriptor.isMapField()) {
            return Lookups.MAP_FIELD_CONSTRAINTS_DESC;
        } else if (fieldDescriptor.isRepeated() && !forItems) {
            return Lookups.REPEATED_FIELD_CONSTRAINTS_DESC;
        } else if (fieldDescriptor.getJavaType() == FieldDescriptor.JavaType.MESSAGE) {
            return Lookups.EXPECTED_WKT_CONSTRAINTS.get(fieldDescriptor.getMessageType().getFullName());
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
                return Decls.newListType(
                        getCELType(fieldDescriptor, true)
                );
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

    public ProgramSet build(Env env, FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, Boolean forItems) throws CompilationError {
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
        AstSet completeSet = new AstSet(prepareEnvironment, new ArrayList<>());
        for (Map.Entry<FieldDescriptor, Object> entry : message.getAllFields().entrySet()) {
            AstSet precomputedAst = loadOrCompileStandardConstraint(prepareEnvironment, entry.getKey());
            completeSet.merge(precomputedAst);
        }
        return completeSet.reduceResiduals(globals(new Variable("rules", message)));
    }
}
