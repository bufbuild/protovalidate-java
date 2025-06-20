// Copyright 2023-2025 Buf Technologies, Inc.
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

import build.buf.validate.Violations;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@link ValidationResult} is returned when a rule is executed. It contains a list of violations.
 * This is non-fatal. If there are no violations, the rule is considered to have passed.
 */
public class ValidationResult {

  /**
   * violations is a list of {@link Violation} that occurred during the validations of a message.
   */
  private final List<Violation> violations;

  /** A violation result with an empty violation list. */
  public static final ValidationResult EMPTY = new ValidationResult(Collections.emptyList());

  /**
   * Creates a violation result from a list of violations.
   *
   * @param violations violation list for the result.
   */
  public ValidationResult(List<Violation> violations) {
    this.violations = violations;
  }

  /**
   * Check if the result is successful.
   *
   * @return if the validation result was a success.
   */
  public boolean isSuccess() {
    return violations.isEmpty();
  }

  /**
   * Get the list of violations in the result.
   *
   * @return the violation list.
   */
  public List<Violation> getViolations() {
    return violations;
  }

  /**
   * Returns a string representation of the validation result, including all the violations.
   *
   * @return a string representation of the validation result.
   */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (isSuccess()) {
      builder.append("Validation OK");
    } else {
      builder.append("Validation error:");
      for (Violation violation : violations) {
        builder.append("\n - ");
        if (violation.toProto().hasField()) {
          builder.append(FieldPathUtils.fieldPathString(violation.toProto().getField()));
          builder.append(": ");
        }
        builder.append(
            String.format(
                "%s [%s]", violation.toProto().getMessage(), violation.toProto().getRuleId()));
      }
    }
    return builder.toString();
  }

  /**
   * Converts the validation result to its equivalent protobuf form.
   *
   * @return The protobuf form of this validation result.
   */
  public build.buf.validate.Violations toProto() {
    List<build.buf.validate.Violation> protoViolations = new ArrayList<>();
    for (Violation violation : violations) {
      protoViolations.add(violation.toProto());
    }
    return Violations.newBuilder().addAllViolations(protoViolations).build();
  }
}
