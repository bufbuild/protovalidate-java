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

import build.buf.protovalidate.exceptions.CompilationException;
import com.google.protobuf.Descriptors.Descriptor;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * ValidatorFactory is used to create a validator.
 *
 * <p>Validators can be created with an optional {@link Config} to customize behavior. They can also
 * be created with a list of seed descriptors to warmup the validator cache ahead of time as well as
 * an indicator to lazily-load any descriptors not provided into the cache.
 */
public final class ValidatorFactory {
  // Prevent instantiation
  private ValidatorFactory() {}

  /** A builder class used for building a validator. */
  public static class ValidatorBuilder {
    /** The config object to use for instantiating a validator. */
    @Nullable private Config config;

    /**
     * Create a validator with the given config
     *
     * @param config The {@link Config} to configure the validator.
     * @return The builder instance
     */
    public ValidatorBuilder withConfig(Config config) {
      this.config = config;
      return this;
    }

    // Prevent instantiation
    private ValidatorBuilder() {}

    /**
     * Build a new validator
     *
     * @return A new {@link Validator} instance.
     */
    public Validator build() {
      Config cfg = this.config;
      if (cfg == null) {
        cfg = Config.newBuilder().build();
      }
      return new ValidatorImpl(cfg);
    }

    /**
     * Build the validator, warming up the cache with any provided descriptors.
     *
     * @param descriptors the list of descriptors to warm up the cache.
     * @param disableLazy whether to disable lazy loading of validation rules. When validation is
     *     performed, a message's rules will be looked up in a cache. If they are not found, by
     *     default they will be processed and lazily-loaded into the cache. Setting this to false
     *     will not attempt to lazily-load descriptor information not found in the cache and
     *     essentially makes the entire cache read-only, eliminating thread contention.
     * @return A new {@link Validator} instance.
     * @throws CompilationException If any of the given descriptors' validation rules fail
     *     processing while warming up the cache.
     * @throws IllegalStateException If disableLazy is set to true and no descriptors are passed.
     */
    public Validator buildWithDescriptors(List<Descriptor> descriptors, boolean disableLazy)
        throws CompilationException, IllegalStateException {
      if (disableLazy && (descriptors == null || descriptors.isEmpty())) {
        throw new IllegalStateException(
            "a list of descriptors is required when disableLazy is true");
      }

      Config cfg = this.config;
      if (cfg == null) {
        cfg = Config.newBuilder().build();
      }
      return new ValidatorImpl(cfg, descriptors, disableLazy);
    }
  }

  /**
   * Creates a new builder for a validator.
   *
   * @return A Validator builder
   */
  public static ValidatorBuilder newBuilder() {
    return new ValidatorBuilder();
  }
}
