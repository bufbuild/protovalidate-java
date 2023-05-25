package build.buf.protovalidatejava;

import build.buf.validate.Constraint;
import build.buf.validate.FieldConstraints;
import build.buf.validate.MessageConstraints;
import build.buf.validate.ValidateProto;
import com.google.protobuf.*;

import java.util.List;
import java.util.Map;

public class Validate {

    public void validate(Message message) {
        MessageConstraints f = message.getDescriptorForType().getOptions().getExtension(ValidateProto.message);
        if (message.getDescriptorForType().isExtendable()) {
            // Need a loop for all present fields. Check for extensions in this loop to
            // validate extensions. The non-extension fields are checked in the loop below.
            return;
        }
        for (Descriptors.FieldDescriptor field: message.getDescriptorForType().getFields()) {
            if (field.getOptions().hasExtension(ValidateProto.field)) {
                FieldConstraints extension = field.getOptions().getExtension(ValidateProto.field);
                List<Constraint> celList = extension.getCelList();
            }
        }
    }
}
