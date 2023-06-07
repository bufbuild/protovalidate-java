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

package build.buf.protovalidate.Constraints;

import build.buf.protovalidate.Errors.ValidationError;
import build.buf.validate.Constraint;
import build.buf.validate.FieldConstraints;
import build.buf.validate.MessageConstraints;
import build.buf.validate.ValidateProto;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.OneofDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import org.projectnessie.cel.checker.Decls;
import org.projectnessie.cel.tools.Script;
import org.projectnessie.cel.tools.ScriptHost;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Constraints implements ConstraintRules {
    private static final Descriptor FIELD_CONSTRAINTS_DESC = FieldConstraints.getDescriptor();
    private static final OneofDescriptor FIELD_CONSTRAINTS_ONEOF_DESC = FIELD_CONSTRAINTS_DESC.getOneofs().get(0);
    private static final FieldDescriptor MAP_FIELD_CONSTRAINTS_DESC = FIELD_CONSTRAINTS_DESC.findFieldByName("map");
    private static final FieldDescriptor REPEATED_FIELD_CONSTRAINTS_DESC = FIELD_CONSTRAINTS_DESC.findFieldByName("repeated");
    private static final Map<FieldDescriptor.Type, FieldDescriptor> EXPECTED_STANDARD_CONSTRAINTS = new HashMap<>();
    private static final Map<String, FieldDescriptor> EXPECTED_WKT_CONSTRAINTS = new HashMap<>();

    static {
        EXPECTED_STANDARD_CONSTRAINTS.put(FieldDescriptor.Type.FLOAT, FIELD_CONSTRAINTS_DESC.findFieldByName("float"));
        EXPECTED_STANDARD_CONSTRAINTS.put(FieldDescriptor.Type.DOUBLE, FIELD_CONSTRAINTS_DESC.findFieldByName("double"));
        EXPECTED_STANDARD_CONSTRAINTS.put(FieldDescriptor.Type.INT32, FIELD_CONSTRAINTS_DESC.findFieldByName("int32"));
        EXPECTED_STANDARD_CONSTRAINTS.put(FieldDescriptor.Type.INT64, FIELD_CONSTRAINTS_DESC.findFieldByName("int64"));
        EXPECTED_STANDARD_CONSTRAINTS.put(FieldDescriptor.Type.UINT32, FIELD_CONSTRAINTS_DESC.findFieldByName("uint32"));
        EXPECTED_STANDARD_CONSTRAINTS.put(FieldDescriptor.Type.UINT64, FIELD_CONSTRAINTS_DESC.findFieldByName("uint64"));
        EXPECTED_STANDARD_CONSTRAINTS.put(FieldDescriptor.Type.SINT32, FIELD_CONSTRAINTS_DESC.findFieldByName("sint32"));
        EXPECTED_STANDARD_CONSTRAINTS.put(FieldDescriptor.Type.SINT64, FIELD_CONSTRAINTS_DESC.findFieldByName("sint64"));
        EXPECTED_STANDARD_CONSTRAINTS.put(FieldDescriptor.Type.FIXED32, FIELD_CONSTRAINTS_DESC.findFieldByName("fixed32"));
        EXPECTED_STANDARD_CONSTRAINTS.put(FieldDescriptor.Type.FIXED64, FIELD_CONSTRAINTS_DESC.findFieldByName("fixed64"));
        EXPECTED_STANDARD_CONSTRAINTS.put(FieldDescriptor.Type.SFIXED32, FIELD_CONSTRAINTS_DESC.findFieldByName("sfixed32"));
        EXPECTED_STANDARD_CONSTRAINTS.put(FieldDescriptor.Type.SFIXED64, FIELD_CONSTRAINTS_DESC.findFieldByName("sfixed64"));
        EXPECTED_STANDARD_CONSTRAINTS.put(FieldDescriptor.Type.BOOL, FIELD_CONSTRAINTS_DESC.findFieldByName("bool"));
        EXPECTED_STANDARD_CONSTRAINTS.put(FieldDescriptor.Type.STRING, FIELD_CONSTRAINTS_DESC.findFieldByName("string"));
        EXPECTED_STANDARD_CONSTRAINTS.put(FieldDescriptor.Type.BYTES, FIELD_CONSTRAINTS_DESC.findFieldByName("bytes"));
        EXPECTED_STANDARD_CONSTRAINTS.put(FieldDescriptor.Type.ENUM, FIELD_CONSTRAINTS_DESC.findFieldByName("enum"));

        EXPECTED_WKT_CONSTRAINTS.put("google.protobuf.Any", FIELD_CONSTRAINTS_DESC.findFieldByName("any"));
        EXPECTED_WKT_CONSTRAINTS.put("google.protobuf.Duration", FIELD_CONSTRAINTS_DESC.findFieldByName("duration"));
        EXPECTED_WKT_CONSTRAINTS.put("google.protobuf.Timestamp", FIELD_CONSTRAINTS_DESC.findFieldByName("timestamp"));
    }

    public static FieldDescriptor expectedWrapperConstraints(String fqn) {
        switch (fqn) {
            case "google.protobuf.BoolValue":
                return EXPECTED_STANDARD_CONSTRAINTS.get(FieldDescriptor.Type.BOOL);
            case "google.protobuf.BytesValue":
                return EXPECTED_STANDARD_CONSTRAINTS.get(FieldDescriptor.Type.BYTES);
            case "google.protobuf.DoubleValue":
                return EXPECTED_STANDARD_CONSTRAINTS.get(FieldDescriptor.Type.DOUBLE);
            case "google.protobuf.FloatValue":
                return EXPECTED_STANDARD_CONSTRAINTS.get(FieldDescriptor.Type.FLOAT);
            case "google.protobuf.Int32Value":
                return EXPECTED_STANDARD_CONSTRAINTS.get(FieldDescriptor.Type.INT32);
            case "google.protobuf.Int64Value":
                return EXPECTED_STANDARD_CONSTRAINTS.get(FieldDescriptor.Type.INT64);
            case "google.protobuf.StringValue":
                return EXPECTED_STANDARD_CONSTRAINTS.get(FieldDescriptor.Type.STRING);
            case "google.protobuf.UInt32Value":
                return EXPECTED_STANDARD_CONSTRAINTS.get(FieldDescriptor.Type.UINT32);
            case "google.protobuf.UInt64Value":
                return EXPECTED_STANDARD_CONSTRAINTS.get(FieldDescriptor.Type.UINT64);
            default:
                return null;
        }
    }

//    public static Type ProtoKindToCELType(FieldDescriptor.Type kind) {
//        switch (kind) {
//            case FLOAT:
//            case DOUBLE:
//                return Type.newBuilder()
//                        .setPrimitive(Type.PrimitiveType.DOUBLE)
//                        .build();
//            case INT32:
//            case INT64:
//            case SINT32:
//            case SINT64:
//            case SFIXED32:
//            case SFIXED64:
//            case ENUM:
//                return Type.newBuilder()
//                        .setPrimitive(Type.PrimitiveType.INT64)
//                        .build();
//            case UINT32:
//            case UINT64:
//            case FIXED32:
//            case FIXED64:
//                return Type.newBuilder()
//                        .setPrimitive(Type.PrimitiveType.UINT64)
//                        .build();
//            case BOOL:
//                return Type.newBuilder()
//                        .setPrimitive(Type.PrimitiveType.BOOL)
//                        .build();
//            case STRING:
//                return Type.newBuilder()
//                        .setPrimitive(Type.PrimitiveType.STRING)
//                        .build();
//            case BYTES:
//                return Type.newBuilder()
//                        .setPrimitive(Type.PrimitiveType.BYTES)
//                        .build();
//            case MESSAGE:
//            case GROUP:
//                return Type.newBuilder()
//                        .setMessageType(kind.getJavaType().name())
//                        .build();
//            default:
//                return Type.newBuilder()
//                        .setPrimitive(Type.PrimitiveType.PRIMITIVE_TYPE_UNSPECIFIED)
//                        .build();
//        }
//    }
//
    private final ScriptHost scriptHost;
    private final Descriptor descriptor;
    private final List<build.buf.protovalidate.Constraints.ConstraintRules> constraints;

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



//    // TODO: potentially in the wrong package, consider moving to expression because ProgramSet is defined there (or move ProgramSet to Constraints package)
//    public ProgramSet build(FieldDescriptor fieldDesc, Message fieldConstraints, boolean forItems) {
//        return null;
//    }
//
//    private CelRuntime prepareEnvironment(CelRuntime env, FieldDescriptor fieldDesc, Message rules, boolean forItems) {
//        return null;
//    }
//
//    // TODO: potentially in the wrong package, consider moving to expression because AstSet is defined there (or move AstSet to Constraints package)
//    private AstSet loadOrCompileStandardConstraint(CelRuntime env, Message constraintField) {
//        return null;
//    }
}