package build.buf.protovalidate.evaluator;

import com.google.protobuf.Descriptors;

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
        // Dynamic programming in a static language.
        return (T) value;
    }
}
