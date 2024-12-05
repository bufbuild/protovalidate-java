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

import javax.annotation.Nullable;

/**
 * {@link Violation} contains all of the collected information about an individual constraint
 * violation.
 */
public class Violation {
  private final build.buf.validate.Violation proto;
  private final @Nullable Value fieldValue;
  private final @Nullable Value ruleValue;

  /** Builds a Violation instance. */
  public static class Builder {
    private build.buf.validate.Violation proto;
    private @Nullable Value fieldValue;
    private @Nullable Value ruleValue;

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
     * @return The builder.
     */
    public Builder setFieldValue(@Nullable Value fieldValue) {
      this.fieldValue = fieldValue;
      return this;
    }

    /**
     * Sets the rule value that corresponds to the violation.
     *
     * @param ruleValue The rule value corresponding to this violation.
     * @return The builder.
     */
    public Builder setRuleValue(@Nullable Value ruleValue) {
      this.ruleValue = ruleValue;
      return this;
    }

    /**
     * Builds a Violation instance with the provided parameters.
     *
     * @return A Violation instance.
     */
    public Violation build() {
      return new Violation(proto, fieldValue, ruleValue);
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
      build.buf.validate.Violation proto, @Nullable Value fieldValue, @Nullable Value ruleValue) {
    this.proto = proto;
    this.fieldValue = fieldValue;
    this.ruleValue = ruleValue;
  }

  /**
   * Gets the protobuf data that corresponds to this constraint violation.
   *
   * @return The protobuf violation data.
   */
  public build.buf.validate.Violation getProto() {
    return proto;
  }

  /**
   * Gets the field value that corresponds to the violation.
   *
   * @return The field value corresponding to this violation.
   */
  public @Nullable Value getFieldValue() {
    return fieldValue;
  }

  /**
   * Gets the rule value that corresponds to the violation.
   *
   * @return The rule value corresponding to this violation.
   */
  public @Nullable Value getRuleValue() {
    return ruleValue;
  }

  /**
   * Constructs a {@link Builder} with all fields set to match this {@link Violation}.
   *
   * @return A new {@link Builder} instance.
   */
  public Builder toBuilder() {
    return new Builder(proto).setFieldValue(fieldValue).setRuleValue(ruleValue);
  }
}
