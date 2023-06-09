package build.buf.protovalidate;

import build.buf.protovalidate.errors.ValidationError;
import build.buf.validate.Violation;

import java.util.Collections;
import java.util.List;

// TODO: revisit this class.
// It might be helpful to have Exception be a specific type.
// value field so far has been null -> success
public class ValidationResult {
    private final ValidationError exception;
    public List<Violation> violations;

    // How does go handle the validation collection
    public ValidationResult(ValidationError validationError) {
        this.exception = validationError;
        this.violations = validationError != null ? validationError.violations : Collections.emptyList();
    }

    public boolean isSuccess() {
        return violations.isEmpty();
    }

    public boolean isFailure() {
        return !isSuccess();
    }

    public ValidationError error() {
        return exception;
    }
}
