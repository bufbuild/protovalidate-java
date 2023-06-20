package build.buf.protovalidate.evaluator;

import com.google.protobuf.Descriptors;
import org.projectnessie.cel.common.ULong;

public class JavaValue {
    private final Descriptors.FieldDescriptor fieldDescriptor;
    private final Object value;

    public JavaValue(Descriptors.FieldDescriptor fieldDescriptor, Object value) {
        this.fieldDescriptor = fieldDescriptor;
        this.value = value;
    }

    public <T> T value() {
        if (fieldDescriptor.isRepeated()) {
            // TODO
        }
        if (fieldDescriptor.isMapField()) {
            // TODO:
        }
        Descriptors.FieldDescriptor.Type type = fieldDescriptor.getType();
        if (type == Descriptors.FieldDescriptor.Type.UINT32
                || type == Descriptors.FieldDescriptor.Type.UINT64
                || type == Descriptors.FieldDescriptor.Type.FIXED32
                || type == Descriptors.FieldDescriptor.Type.FIXED64) {
            /* Java does not have native support for unsigned int/long or uint32/uint64 types.
            To work with CEL's uint type in Java, special handling is required.
            TL;DR: When using uint32/uint64 in your protobuf objects or CEL expressions in Java,
            wrap them with the org.projectnessie.cel.common.ULong type.*/
            return (T) ULong.valueOf(((Number) value).longValue());
        }
        // Dynamic programming in a static language.
        return (T) value;
    }
}
