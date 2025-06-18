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

import build.buf.validate.FieldPath;
import build.buf.validate.FieldPathElement;
import com.google.protobuf.Descriptors;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * {@link RuleViolation} contains all the collected information about an individual rule violation.
 */
final class RuleViolation implements Violation {
  /** Static value to return when there are no violations. */
  static final List<Builder> NO_VIOLATIONS = Collections.emptyList();

  /** {@link FieldValue} represents a Protobuf field value inside a Protobuf message. */
  static class FieldValue implements Violation.FieldValue {
    private final @Nullable Object value;
    private final Descriptors.FieldDescriptor descriptor;

    /**
     * Constructs a {@link FieldValue} from a value and a descriptor directly.
     *
     * @param value Bare Protobuf field value of field.
     * @param descriptor Field descriptor pertaining to this field.
     */
    FieldValue(@Nullable Object value, Descriptors.FieldDescriptor descriptor) {
      this.value = value;
      this.descriptor = descriptor;
    }

    /**
     * Constructs a {@link FieldValue} from a {@link Value}. The value must be for a Protobuf field,
     * e.g. it must have a FieldDescriptor.
     *
     * @param value A {@link Value} to create this {@link FieldValue} from.
     */
    FieldValue(Value value) {
      this.value = value.value(Object.class);
      this.descriptor = Objects.requireNonNull(value.fieldDescriptor());
    }

    @Override
    public @Nullable Object getValue() {
      return value;
    }

    @Override
    public Descriptors.FieldDescriptor getDescriptor() {
      return descriptor;
    }
  }

  private final build.buf.validate.Violation proto;
  private final @Nullable FieldValue fieldValue;
  private final @Nullable FieldValue ruleValue;

  /** Builds a Violation instance. */
  static class Builder {
    private @Nullable String ruleId;
    private @Nullable String message;
    private boolean forKey = false;
    private final Deque<FieldPathElement> fieldPath = new ArrayDeque<>();
    private final Deque<FieldPathElement> rulePath = new ArrayDeque<>();
    private @Nullable FieldValue fieldValue;
    private @Nullable FieldValue ruleValue;

    /**
     * Sets the rule ID field of the resulting violation.
     *
     * @param ruleId Rule ID value to use.
     * @return The builder.
     */
    Builder setRuleId(String ruleId) {
      this.ruleId = ruleId;
      return this;
    }

    /**
     * Sets the message field of the resulting violation.
     *
     * @param message Message value to use.
     * @return The builder.
     */
    Builder setMessage(String message) {
      this.message = message;
      return this;
    }

    /**
     * Sets whether the violation is for a map key or not.
     *
     * @param forKey If true, signals that the resulting violation is for a map key.
     * @return The builder.
     */
    Builder setForKey(boolean forKey) {
      this.forKey = forKey;
      return this;
    }

    /**
     * Adds field path elements to the end of the field path.
     *
     * @param fieldPathElements Field path elements to add.
     * @return The builder.
     */
    Builder addAllFieldPathElements(Collection<? extends FieldPathElement> fieldPathElements) {
      this.fieldPath.addAll(fieldPathElements);
      return this;
    }

    /**
     * Adds a field path element to the beginning of the field path.
     *
     * @param fieldPathElement A field path element to add to the beginning of the field path.
     * @return The builder.
     */
    Builder addFirstFieldPathElement(@Nullable FieldPathElement fieldPathElement) {
      if (fieldPathElement != null) {
        fieldPath.addFirst(fieldPathElement);
      }
      return this;
    }

    /**
     * Adds field path elements to the end of the rule path.
     *
     * @param rulePathElements Field path elements to add.
     * @return The builder.
     */
    Builder addAllRulePathElements(Collection<? extends FieldPathElement> rulePathElements) {
      rulePath.addAll(rulePathElements);
      return this;
    }

    /**
     * Adds a field path element to the beginning of the rule path.
     *
     * @param rulePathElements A field path element to add to the beginning of the rule path.
     * @return The builder.
     */
    Builder addFirstRulePathElement(FieldPathElement rulePathElements) {
      rulePath.addFirst(rulePathElements);
      return this;
    }

    /**
     * Sets the field value that corresponds to the violation.
     *
     * @param fieldValue The field value corresponding to this violation.
     * @return The builder.
     */
    Builder setFieldValue(@Nullable FieldValue fieldValue) {
      this.fieldValue = fieldValue;
      return this;
    }

    /**
     * Sets the rule value that corresponds to the violation.
     *
     * @param ruleValue The rule value corresponding to this violation.
     * @return The builder.
     */
    Builder setRuleValue(@Nullable FieldValue ruleValue) {
      this.ruleValue = ruleValue;
      return this;
    }

    /**
     * Builds a Violation instance with the provided parameters.
     *
     * @return A Violation instance.
     */
    RuleViolation build() {
      build.buf.validate.Violation.Builder protoBuilder = build.buf.validate.Violation.newBuilder();
      if (ruleId != null) {
        protoBuilder.setRuleId(ruleId);
      }
      if (message != null) {
        protoBuilder.setMessage(message);
      }
      if (forKey) {
        protoBuilder.setForKey(true);
      }
      if (!fieldPath.isEmpty()) {
        protoBuilder.setField(FieldPath.newBuilder().addAllElements(fieldPath));
      }
      if (!rulePath.isEmpty()) {
        protoBuilder.setRule(FieldPath.newBuilder().addAllElements(rulePath));
      }
      return new RuleViolation(protoBuilder.build(), fieldValue, ruleValue);
    }

    private Builder() {}
  }

  /**
   * Creates a new empty builder for building a {@link RuleViolation}.
   *
   * @return A new, empty {@link Builder}.
   */
  static Builder newBuilder() {
    return new Builder();
  }

  private RuleViolation(
      build.buf.validate.Violation proto,
      @Nullable FieldValue fieldValue,
      @Nullable FieldValue ruleValue) {
    this.proto = proto;
    this.fieldValue = fieldValue;
    this.ruleValue = ruleValue;
  }

  /**
   * Gets the protobuf data that corresponds to this rule violation.
   *
   * @return The protobuf violation data.
   */
  @Override
  public build.buf.validate.Violation toProto() {
    return proto;
  }

  /**
   * Gets the field value that corresponds to the violation.
   *
   * @return The field value corresponding to this violation.
   */
  @Override
  public @Nullable FieldValue getFieldValue() {
    return fieldValue;
  }

  /**
   * Gets the rule value that corresponds to the violation.
   *
   * @return The rule value corresponding to this violation.
   */
  @Override
  public @Nullable FieldValue getRuleValue() {
    return ruleValue;
  }
}
