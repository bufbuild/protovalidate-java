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

package build.buf.protovalidate.results;

import build.buf.validate.Violation;
import build.buf.validate.Violations;
import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ValidationResult extends RuntimeException {

    public List<Violation> violations;

    public ValidationResult(List<Violation> violations) {
        this.violations = violations;
    }

    public ValidationResult() {
        this.violations = new ArrayList<>();
    }

    public ValidationResult(String s) {
        ValidationResult validationResult = new ValidationResult();
        Violation violation = Violation.newBuilder().setMessage(s).build();
        validationResult.addViolation(violation);
    }

    @Override
    public String getMessage() {
        StringBuilder bldr = new StringBuilder();
        bldr.append("Validation error:");
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

    public Violations asViolations() {
        return Violations.newBuilder()
                .addAllViolations(violations)
                .build();
    }

    public void addViolation(Violation violation) {
        if (violation != null) {
            violations.add(violation);
        }
    }

    public void prefixErrorPaths(String format, Object... args) {
        String prefix = String.format(format, args);

        violations = violations.stream()
                .map(violation -> {
                    String fieldPath = violation.getFieldPath();
                    String prefixedFieldPath;

                    if (fieldPath.isEmpty()) {
                        prefixedFieldPath = prefix;
                    } else if (fieldPath.charAt(0) == '[') {
                        prefixedFieldPath = prefix + fieldPath;
                    } else {
                        prefixedFieldPath = Strings.lenientFormat("%s.%s", prefix, fieldPath);
                    }

                    return violation.toBuilder().setFieldPath(prefixedFieldPath).build();
                })
                .collect(Collectors.toList());
    }

    public boolean isSuccess() {
        return violations.isEmpty();
    }

    public boolean isFailure() {
        return !isSuccess();
    }

    public boolean merge(Exception e, boolean failFast) {
        if (!(e instanceof ValidationResult)) {
            return false;
        }
        ValidationResult source = (ValidationResult) e;
        violations.addAll(source.violations);
        return !(failFast && violations.size() > 0);
    }
}
