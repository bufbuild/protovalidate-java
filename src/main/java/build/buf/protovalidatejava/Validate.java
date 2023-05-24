package build.buf.protovalidatejava;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;

import java.util.Map;

public class Validate {

    public void validate(Message message) {
        for (Map.Entry<Descriptors.FieldDescriptor, Object> field: message.getAllFields().entrySet()) {
            Descriptors.FieldDescriptor fieldDescriptor = field.getKey();

        }
    }
}
