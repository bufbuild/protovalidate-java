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

import build.buf.gen.buf.validate.Violation;
import java.util.Collections;
import java.util.List;

/**
 * {@link ValidationResult} is returned when a constraint is executed. It contains a list of
 * violations. This is non-fatal. If there are no violations, the constraint is considered to have
 * passed.
 */
public class ValidationResult {

  private final List<Violation> violations;

  public ValidationResult() {
    this.violations = Collections.emptyList();
  }

  public ValidationResult(List<Violation> violations) {
    this.violations = violations;
  }

  public boolean isSuccess() {
    return !violations.isEmpty();
  }

  public List<Violation> getViolations() {
    return violations;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Validation error:");
    for (Violation violation : violations) {
      builder.append("\n - ");
      if (!violation.getFieldPath().isEmpty()) {
        builder.append(violation.getFieldPath());
        builder.append(": ");
      }
      builder.append(String.format("%s [%s]", violation.getMessage(), violation.getConstraintId()));
    }
    return builder.toString();
  }
}
