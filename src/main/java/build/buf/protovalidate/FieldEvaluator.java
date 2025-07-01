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

import build.buf.protovalidate.exceptions.ExecutionException;
import build.buf.validate.FieldPath;
import build.buf.validate.FieldRules;
import build.buf.validate.Ignore;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** Performs validation on a single message field, defined by its descriptor. */
final class FieldEvaluator implements Evaluator {
  private static final FieldDescriptor REQUIRED_DESCRIPTOR =
      FieldRules.getDescriptor().findFieldByNumber(FieldRules.REQUIRED_FIELD_NUMBER);

  private static final FieldPath REQUIRED_RULE_PATH =
      FieldPath.newBuilder()
          .addElements(FieldPathUtils.fieldPathElement(REQUIRED_DESCRIPTOR))
          .build();

  private final RuleViolationHelper helper;

  /** The {@link ValueEvaluator} to apply to the field's value */
  final ValueEvaluator valueEvaluator;

  /** The {@link FieldDescriptor} targeted by this evaluator */
  private final FieldDescriptor descriptor;

  /** Indicates that the field must have a set value. */
  private final boolean required;

  /** Whether validation should be ignored for certain conditions */
  private final Ignore ignore;

  /** Whether the field distinguishes between unpopulated and default values. */
  private final boolean hasPresence;

  @Nullable private final Object zero;

  /** Constructs a new {@link FieldEvaluator} */
  FieldEvaluator(
      ValueEvaluator valueEvaluator,
      FieldDescriptor descriptor,
      boolean required,
      boolean hasPresence,
      Ignore ignore,
      @Nullable Object zero) {
    this.helper = new RuleViolationHelper(valueEvaluator);
    this.valueEvaluator = valueEvaluator;
    this.descriptor = descriptor;
    this.required = required;
    this.hasPresence = hasPresence;
    this.ignore = ignore;
    this.zero = zero;
  }

  @Override
  public boolean tautology() {
    return !required && valueEvaluator.tautology();
  }

  /**
   * Returns whether a field should always skip validation.
   *
   * <p>If true, this will take precedence and all checks are skipped.
   */
  private boolean shouldIgnoreAlways() {
    return this.ignore == Ignore.IGNORE_ALWAYS;
  }

  /**
   * Returns whether a field should skip validation on its zero value.
   *
   * <p>This is generally true for nullable fields or fields with the ignore_empty rule explicitly
   * set.
   */
  private boolean shouldIgnoreEmpty() {
    return this.hasPresence
        || this.ignore == Ignore.IGNORE_IF_UNPOPULATED
        || this.ignore == Ignore.IGNORE_IF_DEFAULT_VALUE;
  }

  /**
   * Returns whether a field should skip validation on its zero value, including for fields which
   * have field presence and are set to the zero value.
   */
  private boolean shouldIgnoreDefault() {
    return this.hasPresence && this.ignore == Ignore.IGNORE_IF_DEFAULT_VALUE;
  }

  @Override
  public List<RuleViolation.Builder> evaluate(Value val, boolean failFast)
      throws ExecutionException {
    if (this.shouldIgnoreAlways()) {
      return RuleViolation.NO_VIOLATIONS;
    }
    MessageReflector message = val.messageValue();
    if (message == null) {
      return RuleViolation.NO_VIOLATIONS;
    }
    boolean hasField;
    if (descriptor.isRepeated()) {
      if (descriptor.isMapField()) {
        hasField = !message.getField(descriptor).mapValue().isEmpty();
      } else {
        hasField = !message.getField(descriptor).repeatedValue().isEmpty();
      }
    } else {
      hasField = message.hasField(descriptor);
    }
    if (required && !hasField) {
      return Collections.singletonList(
          RuleViolation.newBuilder()
              .addFirstFieldPathElement(FieldPathUtils.fieldPathElement(descriptor))
              .addAllRulePathElements(helper.getRulePrefixElements())
              .addAllRulePathElements(REQUIRED_RULE_PATH.getElementsList())
              .setRuleId("required")
              .setMessage("value is required")
              .setRuleValue(new RuleViolation.FieldValue(true, REQUIRED_DESCRIPTOR)));
    }
    if (this.shouldIgnoreEmpty() && !hasField) {
      return RuleViolation.NO_VIOLATIONS;
    }
    Value fieldValue = message.getField(descriptor);
    if (this.shouldIgnoreDefault() && Objects.equals(zero, fieldValue.jvmValue(Object.class))) {
      return RuleViolation.NO_VIOLATIONS;
    }
    return valueEvaluator.evaluate(fieldValue, failFast);
  }
}
