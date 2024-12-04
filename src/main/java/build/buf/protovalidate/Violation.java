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

import java.util.Objects;
import javax.annotation.Nullable;

/**
 * {@link Violation} contains all of the collected information about an individual constraint
 * violation.
 */
public class Violation {
  private final build.buf.validate.Violation proto;

  /** Builds a Violation instance. */
  public static class Builder {
    @Nullable private build.buf.validate.Violation proto;

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
     * Builds a Violation instance with the provided parameters.
     *
     * @return A Violation instance.
     */
    public Violation build() {
      return new Violation(Objects.requireNonNull(proto));
    }

    private Builder() {}
  }

  /**
   * Constructs a new empty builder.
   *
   * @return A new empty builder instance.
   */
  public static Builder newBuilder() {
    return new Builder();
  }

  Violation(build.buf.validate.Violation proto) {
    this.proto = proto;
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
   * Constructs a {@link Builder} with all fields set to match this {@link Violation}.
   *
   * @return A new {@link Builder} instance.
   */
  public Builder toBuilder() {
    Builder builder = new Builder();
    builder.setProto(proto);
    return builder;
  }
}
