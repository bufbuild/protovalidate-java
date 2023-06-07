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

package build.buf.protovalidate.errors;

import build.buf.validate.Violation;
import build.buf.validate.Violations;

import java.util.Collections;
import java.util.List;

public class ValidationError extends RuntimeException {

    private List<Violation> violations;

    public ValidationError(List<Violation> violations) {
        super("Validation error:");
        this.violations = violations;
    }

    public ValidationError() {
        this.violations = Collections.emptyList();
    }

    @Override
    public String getMessage() {
        StringBuilder bldr = new StringBuilder();
        bldr.append(super.getMessage());
        for (Violation violation : violations) {
            bldr.append("\n - ");
            if (!violation.getFieldPath().isEmpty()) {
                bldr.append(violation.getFieldPath());
                bldr.append(": ");
            }
            bldr.append(String.format("%s [%s]", violation.getMessage(), violation.getConstraintId()));
        }
        return bldr.toString();
    }

    public Violations toProto() {
        return null;
    }

    public void prefixFieldPaths(String format, Object... args) {}
}
