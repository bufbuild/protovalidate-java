package build.buf.protovalidate.constraints;

import build.buf.validate.FieldConstraints;
import com.google.api.expr.v1alpha1.Type;
import com.google.protobuf.Descriptors;

import java.util.HashMap;
import java.util.Map;

public class Lookups {

    static final Descriptors.Descriptor FIELD_CONSTRAINTS_DESC = FieldConstraints.getDescriptor();
    static final Descriptors.OneofDescriptor FIELD_CONSTRAINTS_ONEOF_DESC = FIELD_CONSTRAINTS_DESC.getOneofs().get(0);
    static final Descriptors.FieldDescriptor MAP_FIELD_CONSTRAINTS_DESC = FIELD_CONSTRAINTS_DESC.findFieldByName("map");
    static final Descriptors.FieldDescriptor REPEATED_FIELD_CONSTRAINTS_DESC = FIELD_CONSTRAINTS_DESC.findFieldByName("repeated");
    static final Map<Descriptors.FieldDescriptor.Type, Descriptors.FieldDescriptor> EXPECTED_STANDARD_CONSTRAINTS = new HashMap<>();
    static final Map<String, Descriptors.FieldDescriptor> EXPECTED_WKT_CONSTRAINTS = new HashMap<>();

    static {
        EXPECTED_STANDARD_CONSTRAINTS.put(Descriptors.FieldDescriptor.Type.FLOAT, FIELD_CONSTRAINTS_DESC.findFieldByName("float"));
        EXPECTED_STANDARD_CONSTRAINTS.put(Descriptors.FieldDescriptor.Type.DOUBLE, FIELD_CONSTRAINTS_DESC.findFieldByName("double"));
        EXPECTED_STANDARD_CONSTRAINTS.put(Descriptors.FieldDescriptor.Type.INT32, FIELD_CONSTRAINTS_DESC.findFieldByName("int32"));
        EXPECTED_STANDARD_CONSTRAINTS.put(Descriptors.FieldDescriptor.Type.INT64, FIELD_CONSTRAINTS_DESC.findFieldByName("int64"));
        EXPECTED_STANDARD_CONSTRAINTS.put(Descriptors.FieldDescriptor.Type.UINT32, FIELD_CONSTRAINTS_DESC.findFieldByName("uint32"));
        EXPECTED_STANDARD_CONSTRAINTS.put(Descriptors.FieldDescriptor.Type.UINT64, FIELD_CONSTRAINTS_DESC.findFieldByName("uint64"));
        EXPECTED_STANDARD_CONSTRAINTS.put(Descriptors.FieldDescriptor.Type.SINT32, FIELD_CONSTRAINTS_DESC.findFieldByName("sint32"));
        EXPECTED_STANDARD_CONSTRAINTS.put(Descriptors.FieldDescriptor.Type.SINT64, FIELD_CONSTRAINTS_DESC.findFieldByName("sint64"));
        EXPECTED_STANDARD_CONSTRAINTS.put(Descriptors.FieldDescriptor.Type.FIXED32, FIELD_CONSTRAINTS_DESC.findFieldByName("fixed32"));
        EXPECTED_STANDARD_CONSTRAINTS.put(Descriptors.FieldDescriptor.Type.FIXED64, FIELD_CONSTRAINTS_DESC.findFieldByName("fixed64"));
        EXPECTED_STANDARD_CONSTRAINTS.put(Descriptors.FieldDescriptor.Type.SFIXED32, FIELD_CONSTRAINTS_DESC.findFieldByName("sfixed32"));
        EXPECTED_STANDARD_CONSTRAINTS.put(Descriptors.FieldDescriptor.Type.SFIXED64, FIELD_CONSTRAINTS_DESC.findFieldByName("sfixed64"));
        EXPECTED_STANDARD_CONSTRAINTS.put(Descriptors.FieldDescriptor.Type.BOOL, FIELD_CONSTRAINTS_DESC.findFieldByName("bool"));
        EXPECTED_STANDARD_CONSTRAINTS.put(Descriptors.FieldDescriptor.Type.STRING, FIELD_CONSTRAINTS_DESC.findFieldByName("string"));
        EXPECTED_STANDARD_CONSTRAINTS.put(Descriptors.FieldDescriptor.Type.BYTES, FIELD_CONSTRAINTS_DESC.findFieldByName("bytes"));
        EXPECTED_STANDARD_CONSTRAINTS.put(Descriptors.FieldDescriptor.Type.ENUM, FIELD_CONSTRAINTS_DESC.findFieldByName("enum"));
        EXPECTED_WKT_CONSTRAINTS.put("google.protobuf.Any", FIELD_CONSTRAINTS_DESC.findFieldByName("any"));
        EXPECTED_WKT_CONSTRAINTS.put("google.protobuf.Duration", FIELD_CONSTRAINTS_DESC.findFieldByName("duration"));
        EXPECTED_WKT_CONSTRAINTS.put("google.protobuf.Timestamp", FIELD_CONSTRAINTS_DESC.findFieldByName("timestamp"));
    }

    public static Descriptors.FieldDescriptor expectedWrapperConstraints(String fqn) {
        switch (fqn) {
            case "google.protobuf.BoolValue":
                return EXPECTED_STANDARD_CONSTRAINTS.get(Descriptors.FieldDescriptor.Type.BOOL);
            case "google.protobuf.BytesValue":
                return EXPECTED_STANDARD_CONSTRAINTS.get(Descriptors.FieldDescriptor.Type.BYTES);
            case "google.protobuf.DoubleValue":
                return EXPECTED_STANDARD_CONSTRAINTS.get(Descriptors.FieldDescriptor.Type.DOUBLE);
            case "google.protobuf.FloatValue":
                return EXPECTED_STANDARD_CONSTRAINTS.get(Descriptors.FieldDescriptor.Type.FLOAT);
            case "google.protobuf.Int32Value":
                return EXPECTED_STANDARD_CONSTRAINTS.get(Descriptors.FieldDescriptor.Type.INT32);
            case "google.protobuf.Int64Value":
                return EXPECTED_STANDARD_CONSTRAINTS.get(Descriptors.FieldDescriptor.Type.INT64);
            case "google.protobuf.StringValue":
                return EXPECTED_STANDARD_CONSTRAINTS.get(Descriptors.FieldDescriptor.Type.STRING);
            case "google.protobuf.UInt32Value":
                return EXPECTED_STANDARD_CONSTRAINTS.get(Descriptors.FieldDescriptor.Type.UINT32);
            case "google.protobuf.UInt64Value":
                return EXPECTED_STANDARD_CONSTRAINTS.get(Descriptors.FieldDescriptor.Type.UINT64);
            default:
                return null;
        }
    }

    public static Type protoKindToCELType(Descriptors.FieldDescriptor.Type kind) {
        switch (kind) {
            case FLOAT:
            case DOUBLE:
                return Type.newBuilder()
                        .setPrimitive(Type.PrimitiveType.DOUBLE)
                        .build();
            case INT32:
            case INT64:
            case SINT32:
            case SINT64:
            case SFIXED32:
            case SFIXED64:
            case ENUM:
                return Type.newBuilder()
                        .setPrimitive(Type.PrimitiveType.INT64)
                        .build();
            case UINT32:
            case UINT64:
            case FIXED32:
            case FIXED64:
                return Type.newBuilder()
                        .setPrimitive(Type.PrimitiveType.UINT64)
                        .build();
            case BOOL:
                return Type.newBuilder()
                        .setPrimitive(Type.PrimitiveType.BOOL)
                        .build();
            case STRING:
                return Type.newBuilder()
                        .setPrimitive(Type.PrimitiveType.STRING)
                        .build();
            case BYTES:
                return Type.newBuilder()
                        .setPrimitive(Type.PrimitiveType.BYTES)
                        .build();
            case MESSAGE:
            case GROUP:
                return Type.newBuilder()
                        .setMessageType(kind.getJavaType().name())
                        .build();
            default:
                return Type.newBuilder()
                        .setPrimitive(Type.PrimitiveType.PRIMITIVE_TYPE_UNSPECIFIED)
                        .build();
        }
    }
}
