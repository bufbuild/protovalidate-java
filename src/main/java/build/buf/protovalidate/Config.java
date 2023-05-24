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

/** Config is the configuration for a Validator. */
public final class Config {
  private final boolean failFast;
  private final boolean disableLazy;

  private Config(boolean failFast, boolean disableLazy) {
    this.failFast = failFast;
    this.disableLazy = disableLazy;
  }

  /**
   * Create a new Configuration builder.
   *
   * @return a new Configuration builder.
   */
  public static Builder newBuilder() {
    return new Builder();
  }

  /**
   * Checks if the configuration for failing fast is enabled.
   *
   * @return if failing fast is enabled
   */
  public boolean isFailFast() {
    return failFast;
  }

  /**
   * Checks if the configuration for disabling lazy evaluation is enabled.
   *
   * @return if disabling lazy evaluation is enabled
   */
  public boolean isDisableLazy() {
    return disableLazy;
  }

  /** Builder for configuration. Provides a forward compatible API for users. */
  public static final class Builder {
    private boolean failFast;
    private boolean disableLazy;

    private Builder() {}

    /**
     * Set the configuration for failing fast.
     *
     * @param failFast the boolean for enabling
     * @return this builder
     */
    public Builder setFailFast(boolean failFast) {
      this.failFast = failFast;
      return this;
    }

    /**
     * Set the configuration for disabling lazy evaluation.
     *
     * @param disableLazy the boolean for enabling
     * @return this builder
     */
    public Builder setDisableLazy(boolean disableLazy) {
      this.disableLazy = disableLazy;
      return this;
    }

    /**
     * Build the corresponding {@link Config}.
     *
     * @return the configuration.
     */
    public Config build() {
      return new Config(failFast, disableLazy);
    }
  }
}
