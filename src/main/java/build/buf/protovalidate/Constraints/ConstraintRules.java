package build.buf.protovalidate.Constraints;

import com.google.protobuf.Message;

public interface ConstraintRules {
    boolean validate(String fieldPath, Message message);
}
