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
import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.List;

public class ValidationError extends RuntimeException {

    public List<Violation> violations;

    public ValidationError(List<Violation> violations) {
        this.violations = violations;
    }

    public ValidationError() {
        this.violations = new ArrayList<>();
    }

    public ValidationError(String s) {
        this.violations = new ArrayList<>();
        Violation violation = Violation.newBuilder().setMessage(s).build();
        ValidationError err = new ValidationError();
        err.addViolation(violation);
        this.violations.add(violation);
    }

    @Override
    public String getMessage() {
        StringBuilder bldr = new StringBuilder();
        bldr.append("Validation error:");
        for (Violation violation : violations) {
            if (!violation.getFieldPath().isEmpty()) {
                bldr.append(violation.getFieldPath());
                bldr.append(": ");
            }
            bldr.append(String.format("%s [%s]", violation.getMessage(), violation.getConstraintId()));
            bldr.append("\n - ");
        }
        return bldr.toString();
    }

    public Violations asViolations() {
        return Violations.newBuilder()
                .addAllViolations(violations)
                .build();
    }

    public void addViolation(Violation violation) {
        this.violations.add(violation);
    }

    public int getViolationsCount() {
        return this.violations.size();
    }

    public void prefixErrorPaths(String fullName, Integer index) {
        String prefix = index == null ? fullName : String.format(fullName, index);
        // TODO: not a fan of this approach but it's copying go to make things work.
        List<Violation> prefixedViolations = new ArrayList<>();
        for (Violation violation : violations) {
            Violation prefixedViolation;
            if (violation.getFieldPath().isEmpty()) {
                prefixedViolation = violation.toBuilder().setFieldPath(prefix).build();
            } else if (violation.getFieldPath().charAt(0) == '[') {
                prefixedViolation = violation.toBuilder().setFieldPath(prefix + violation.getFieldPath()).build();
            } else {
                prefixedViolation = violation.toBuilder().setFieldPath(Strings.lenientFormat("%s.%s", prefix, violation.getFieldPath())).build();
            }
            prefixedViolations.add(prefixedViolation);
        }
        this.violations = prefixedViolations;
    }

    public boolean isEmpty() {
        return violations.isEmpty();
    }
}
