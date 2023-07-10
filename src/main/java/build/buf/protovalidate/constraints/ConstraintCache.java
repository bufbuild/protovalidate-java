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

import build.buf.gen.buf.validate.FieldConstraints;
import build.buf.protovalidate.expression.CompiledProgram;
import build.buf.protovalidate.expression.Variable;
import build.buf.protovalidate.results.CompilationException;
import com.google.api.expr.v1alpha1.Type;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import org.projectnessie.cel.Env;
import org.projectnessie.cel.EnvOption;
import org.projectnessie.cel.checker.Decls;

import java.util.List;

/**
 * ConstraintCache is a build-through cache to computed standard constraints.
 */
public class ConstraintCache {
    private final Env env;

    /**
     * Constructs a new build-through cache for the standard constraints.
     */
    public ConstraintCache(Env env) {
        this.env = env;
    }

    /**
     * Creates the standard constraints for the given field. If forItems is
     * true, the constraints for repeated list items is built instead of the
     * constraints on the list itself.
     */
    public List<CompiledProgram> compile(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, Boolean forItems) throws CompilationException {
        Message message = resolveConstraints(fieldDescriptor, fieldConstraints, forItems);
        if (message == null) {
            // Message null means there were no constraints resolved.
            return null;
        }
        Env prepareEnvironment = prepareEnvironment(fieldDescriptor, message, forItems);
        return CompiledProgram.compile(prepareEnvironment, message);
    }

    /**
     * Extracts the standard constraints for the specified field. An
     * exception is thrown if the wrong constraints are applied to a field (typically
     * if there is a type-mismatch). Null is returned if there are no standard constraints
     * to apply to this field.
     */
    private Message resolveConstraints(FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, Boolean forItems) throws CompilationException {
        // Get the oneof field descriptor from the field constraints.
        FieldDescriptor oneofFieldDescriptor = fieldConstraints.getOneofFieldDescriptor(DescriptorMappings.FIELD_CONSTRAINTS_ONEOF_DESC);
        if (oneofFieldDescriptor == null) {
            // If the oneof field descriptor is null there are no constraints to resolve.
            return null;
        }

        // Get the expected constraint descriptor based on the provided field descriptor and the flag indicating whether it is for items.
        FieldDescriptor expectedConstraintDescriptor = getExpectedConstraintDescriptor(fieldDescriptor, forItems);
        boolean ok = expectedConstraintDescriptor != null;
        if (ok && !oneofFieldDescriptor.getFullName().equals(expectedConstraintDescriptor.getFullName())) {
            // If the expected constraint does not match the actual oneof constraint, throw a CompilationError.
            throw new CompilationException("expected constraint %s, got %s on field %s",
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

    /**
     * Prepares the environment for compiling standard constraint expressions.
     */
    private Env prepareEnvironment(FieldDescriptor fieldDesc, Message rules, Boolean forItems) {
        return env.extend(
                EnvOption.types(rules.getDefaultInstanceForType()),
                EnvOption.declarations(
                        Decls.newVar(Variable.THIS_NAME, getCELType(fieldDesc, forItems)),
                        Decls.newVar(Variable.RULES_NAME, Decls.newObjectType(rules.getDescriptorForType().getFullName()))
                )
        );
    }

    /**
     * Produces the field descriptor from the build.buf.gen.buf.validate.FieldConstraints 'type' oneof that
     * matches the provided target field descriptor. If the returned value is null, the field does not
     * expect any standard constraints.
     */
    private FieldDescriptor getExpectedConstraintDescriptor(FieldDescriptor fieldDescriptor, Boolean forItems) {
        if (fieldDescriptor.isMapField()) {
            return DescriptorMappings.MAP_FIELD_CONSTRAINTS_DESC;
        } else if (fieldDescriptor.isRepeated() && !forItems) {
            return DescriptorMappings.REPEATED_FIELD_CONSTRAINTS_DESC;
        } else if (fieldDescriptor.getJavaType() == FieldDescriptor.JavaType.MESSAGE) {
            return DescriptorMappings.EXPECTED_WKT_CONSTRAINTS.get(fieldDescriptor.getMessageType().getFullName());
        } else {
            return DescriptorMappings.EXPECTED_STANDARD_CONSTRAINTS.get(fieldDescriptor.getType());
        }
    }

    /**
     * Resolves the CEL value type for the provided FieldDescriptor. If
     * forItems is true, the type for the repeated list items is returned instead of
     * the list type itself.
     */
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
        return DescriptorMappings.protoKindToCELType(fieldDescriptor.getType());
    }
}
