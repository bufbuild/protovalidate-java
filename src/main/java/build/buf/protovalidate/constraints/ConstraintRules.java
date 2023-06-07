package build.buf.protovalidate.constraints;

import com.google.protobuf.Message;

public interface ConstraintRules {
    boolean validate(String fieldPath, Message message);
}
