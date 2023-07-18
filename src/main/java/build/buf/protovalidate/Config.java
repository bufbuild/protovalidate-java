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
  /**
   * Specifies whether validation should fail on the first constraint violation encountered or if
   * all violations should be accumulated. By default, all violations are accumulated.
   */
  private final boolean failFast;
  /**
   * Prevents the Validator from lazily building validation logic for a message it has not
   * encountered before. Disabling lazy logic additionally eliminates any internal locking as the
   * validator becomes read-only. Note: All expected messages must be provided by WithMessages or
   * WithDescriptors during initialization.
   */
  private final boolean disableLazy;

  /** Config constructs a new Config. */
  private Config(boolean failFast, boolean disableLazy) {
    this.failFast = failFast;
    this.disableLazy = disableLazy;
  }

  public static Builder builder() {
    return new Builder();
  }

  public boolean isFailFast() {
    return failFast;
  }

  public boolean isDisableLazy() {
    return disableLazy;
  }

  public static final class Builder {
    private boolean failFast;
    private boolean disableLazy;

    private Builder() {}

    public Builder setFailFast(boolean failFast) {
      this.failFast = failFast;
      return this;
    }

    public Builder setDisableLazy(boolean disableLazy) {
      this.disableLazy = disableLazy;
      return this;
    }

    public Config build() {
      return new Config(failFast, disableLazy);
    }
  }
}
