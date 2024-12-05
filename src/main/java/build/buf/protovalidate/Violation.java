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

package build.buf.protovalidate;

import com.google.protobuf.Descriptors;
import javax.annotation.Nullable;

/**
 * {@link Violation} contains all of the collected information about an individual constraint
 * violation.
 */
public class Violation {
  private final build.buf.validate.Violation proto;
  private final @Nullable Object fieldValue;
  private final @Nullable Descriptors.FieldDescriptor fieldDescriptor;
  private final @Nullable Object ruleValue;
  private final @Nullable Descriptors.FieldDescriptor ruleDescriptor;

  /** Builds a Violation instance. */
  public static class Builder {
    private build.buf.validate.Violation proto;
    private @Nullable Object fieldValue;
    private @Nullable Descriptors.FieldDescriptor fieldDescriptor;
    private @Nullable Object ruleValue;
    private @Nullable Descriptors.FieldDescriptor ruleDescriptor;

    /**
     * Sets the underlying protobuf message that corresponds to the violation.
     *
     * @param proto The value to set for this field.
     * @return The builder.
     */
    public Builder setProto(build.buf.validate.Violation proto) {
      this.proto = proto;
      return this;
    }

    /**
     * Sets the field value that corresponds to the violation.
     *
     * @param fieldValue The field value corresponding to this violation.
     * @param fieldDescriptor The field descriptor corresponding to the field value.
     * @return The builder.
     */
    public Builder setFieldValue(
        @Nullable Object fieldValue, @Nullable Descriptors.FieldDescriptor fieldDescriptor) {
      this.fieldValue = fieldValue;
      this.fieldDescriptor = fieldDescriptor;
      return this;
    }

    /**
     * Sets the rule value that corresponds to the violation.
     *
     * @param ruleValue The rule value corresponding to this violation.
     * @param ruleDescriptor The field descriptor corresponding to the rule value.
     * @return The builder.
     */
    public Builder setRuleValue(
        @Nullable Object ruleValue, @Nullable Descriptors.FieldDescriptor ruleDescriptor) {
      this.ruleValue = ruleValue;
      this.ruleDescriptor = ruleDescriptor;
      return this;
    }

    /**
     * Builds a Violation instance with the provided parameters.
     *
     * @return A Violation instance.
     */
    public Violation build() {
      return new Violation(proto, fieldValue, fieldDescriptor, ruleValue, ruleDescriptor);
    }

    private Builder(build.buf.validate.Violation proto) {
      this.proto = proto;
    }
  }

  /**
   * Constructs a new empty builder.
   *
   * @param proto The proto Violation to wrap.
   * @return A new empty builder instance.
   */
  public static Builder newBuilder(build.buf.validate.Violation proto) {
    return new Builder(proto);
  }

  private Violation(
      build.buf.validate.Violation proto,
      @Nullable Object fieldValue,
      @Nullable Descriptors.FieldDescriptor fieldDescriptor,
      @Nullable Object ruleValue,
      @Nullable Descriptors.FieldDescriptor ruleDescriptor) {
    this.proto = proto;
    this.fieldValue = fieldValue;
    this.fieldDescriptor = fieldDescriptor;
    this.ruleValue = ruleValue;
    this.ruleDescriptor = ruleDescriptor;
  }

  /**
   * Gets the protobuf data that corresponds to this constraint violation.
   *
   * @return The protobuf violation data.
   */
  public build.buf.validate.Violation toProto() {
    return proto;
  }

  /**
   * Gets the field value that corresponds to the violation.
   *
   * @return The field value corresponding to this violation.
   */
  public @Nullable Object getFieldValue() {
    return fieldValue;
  }

  /**
   * Gets the field descriptor that corresponds to the field value.
   *
   * @return The field descriptor that corresponds to the field value.
   */
  public @Nullable Descriptors.FieldDescriptor getFieldDescriptor() {
    return fieldDescriptor;
  }

  /**
   * Gets the rule value that corresponds to the violation.
   *
   * @return The rule value corresponding to this violation.
   */
  public @Nullable Object getRuleValue() {
    return ruleValue;
  }

  /**
   * Gets the field descriptor that corresponds to the rule value.
   *
   * @return The field descriptor that corresponds to the rule value.
   */
  public @Nullable Descriptors.FieldDescriptor getRuleDescriptor() {
    return ruleDescriptor;
  }

  /**
   * Constructs a {@link Builder} with all fields set to match this {@link Violation}.
   *
   * @return A new {@link Builder} instance.
   */
  public Builder toBuilder() {
    return new Builder(proto)
        .setFieldValue(fieldValue, fieldDescriptor)
        .setRuleValue(ruleValue, ruleDescriptor);
  }
}
