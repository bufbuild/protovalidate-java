// Copyright 2023-2024 Buf Technologies, Inc.
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

package build.buf.protovalidate.internal.evaluator;

import build.buf.protovalidate.ValidationResult;
import build.buf.protovalidate.Violation;
import build.buf.protovalidate.exceptions.ExecutionException;
import build.buf.protovalidate.internal.errors.FieldPathUtils;
import build.buf.validate.FieldConstraints;
import build.buf.validate.FieldPath;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

/** Performs validation on a single message field, defined by its descriptor. */
class FieldEvaluator implements Evaluator {
  private static final FieldDescriptor REQUIRED_DESCRIPTOR =
      FieldConstraints.getDescriptor().findFieldByNumber(FieldConstraints.REQUIRED_FIELD_NUMBER);

  private static final FieldPath REQUIRED_RULE_PATH =
      FieldPath.newBuilder()
          .addElements(FieldPathUtils.fieldPathElement(REQUIRED_DESCRIPTOR))
          .build();

  /** The {@link ValueEvaluator} to apply to the field's value */
  public final ValueEvaluator valueEvaluator;

  /** The {@link FieldDescriptor} targeted by this evaluator */
  private final FieldDescriptor descriptor;

  /** Indicates that the field must have a set value. */
  private final boolean required;

  /**
   * ignoreEmpty indicates if a field should skip validation on its zero value. This field is
   * generally true for nullable fields or fields with the ignore_empty constraint explicitly set.
   */
  private final boolean ignoreEmpty;

  private final boolean ignoreDefault;

  @Nullable private final Object zero;

  /** Constructs a new {@link FieldEvaluator} */
  FieldEvaluator(
      ValueEvaluator valueEvaluator,
      FieldDescriptor descriptor,
      boolean required,
      boolean ignoreEmpty,
      boolean ignoreDefault,
      @Nullable Object zero) {
    this.valueEvaluator = valueEvaluator;
    this.descriptor = descriptor;
    this.required = required;
    this.ignoreEmpty = ignoreEmpty;
    this.ignoreDefault = ignoreDefault;
    this.zero = zero;
  }

  @Override
  public boolean tautology() {
    return !required && valueEvaluator.tautology();
  }

  @Override
  public ValidationResult evaluate(Value val, boolean failFast) throws ExecutionException {
    Message message = val.messageValue();
    if (message == null) {
      return ValidationResult.EMPTY;
    }
    boolean hasField;
    if (descriptor.isRepeated()) {
      hasField = message.getRepeatedFieldCount(descriptor) != 0;
    } else {
      hasField = message.hasField(descriptor);
    }
    if (required && !hasField) {
      return new ValidationResult(
          Collections.singletonList(
              Violation.newBuilder(
                      build.buf.validate.Violation.newBuilder()
                          .setField(
                              FieldPath.newBuilder()
                                  .addElements(FieldPathUtils.fieldPathElement(descriptor))
                                  .build())
                          .setRule(REQUIRED_RULE_PATH)
                          .setConstraintId("required")
                          .setMessage("value is required")
                          .build())
                  .setFieldValue(val.value(Object.class), val.fieldDescriptor())
                  .setRuleValue(true, REQUIRED_DESCRIPTOR)
                  .build()));
    }
    if (ignoreEmpty && !hasField) {
      return ValidationResult.EMPTY;
    }
    Object fieldValue = message.getField(descriptor);
    if (ignoreDefault && Objects.equals(zero, fieldValue)) {
      return ValidationResult.EMPTY;
    }
    ValidationResult evalResult =
        valueEvaluator.evaluate(new ObjectValue(descriptor, fieldValue), failFast);
    List<Violation> violations =
        FieldPathUtils.prependFieldPaths(
            evalResult.getViolations(),
            FieldPathUtils.fieldPathElement(descriptor).build(),
            descriptor.isRepeated());
    return new ValidationResult(violations);
  }
}
