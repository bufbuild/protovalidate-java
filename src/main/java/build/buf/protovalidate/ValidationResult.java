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

    public void prefixErrorPaths(String fullName) {
        exception.prefixErrorPaths(fullName);
    }
}
