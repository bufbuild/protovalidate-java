package build.buf.protovalidate;

import build.buf.protovalidate.errors.CompilationError;
import build.buf.protovalidate.errors.ValidationError;
import build.buf.validate.Violation;

import java.util.Collections;
import java.util.List;

// TODO: revisit this class.
// It might be helpful to have Exception be a specific type.
// value field so far has been null -> success
public class ValidationResult {
    private final ValidationError exception;
    public boolean isValid;

    // How does go handle the validation collection
    public ValidationResult(ValidationError validationError) {
        this.exception = validationError;
        this.isValid = validationError == null || validationError.violations.isEmpty();
    }

    public ValidationResult(CompilationError e) {
        this.exception = new ValidationError();
    }

    public boolean isSuccess() {
        return isValid;
    }

    public boolean isFailure() {
        return !isSuccess();
    }

    public ValidationError error() {
        return exception;
    }

    public static ValidationResult success() {
        return new ValidationResult((ValidationError) null);
    }
}
