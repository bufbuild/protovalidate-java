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

import build.buf.protovalidate.exceptions.CompilationException;
import com.google.protobuf.Descriptors.Descriptor;
import java.util.Collections;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** ValidatorFactory is used to create a validator */
public class ValidatorFactory {

  public static class EagerBuilder extends BaseBuilder<EagerBuilder> {
    private final List<Descriptor> descriptors;
    private final boolean disableLazy;

    /**
     * An eager builder behaves the same as a regular Builder, but attempts to warmup the validator
     * cache with a given list of Descriptors.
     */
    EagerBuilder(List<Descriptor> descriptors, boolean disableLazy) {
      this.descriptors = Collections.unmodifiableList(descriptors);
      this.disableLazy = disableLazy;
    }

    /**
     * Build the validator, warming up the cache with any provided descriptors.
     *
     * @return A Validator instance
     * @throws CompilationException If any of the given descriptors' validation rules fail
     *     processing while warming up the cache.
     * @throws IllegalStateException If disableLazy is set to true and no descriptors are passed.
     */
    public Validator build() throws CompilationException, IllegalStateException {
      if (disableLazy && this.descriptors.size() == 0) {
        throw new IllegalStateException();
      }

      Config cfg = this.config;
      if (cfg == null) {
        cfg = Config.newBuilder().build();
      }
      return new ValidatorImpl(cfg);
    }

    @Override
    EagerBuilder self() {
      return this;
    }

    boolean getDisableLazy() {
      return this.disableLazy;
    }

    List<Descriptor> getDescriptors() {
      return this.descriptors;
    }
  }

  public static class Builder extends BaseBuilder<Builder> {
    public Validator build() {
      Config cfg = this.config;
      if (cfg == null) {
        cfg = Config.newBuilder().build();
      }
      return new ValidatorImpl(cfg);
    }

    @Override
    Builder self() {
      return this;
    }
  }

  /** 
   * A convenience function for creating a new validator with a default configuration. 
   *
   * @return A Validator instance
   **/
  public static Validator defaultInstance() {
    return newBuilder().build();
  }

  /** 
   * Creates a new builder for a validator. 
   *
   * @return A Validator instance
   **/
  public static Builder newBuilder() {
    return new Builder();
  }

  /**
   * Creates a new eager builder for a validator.
   *
   * @param descriptors the list of descriptors to warm up the cache.
   * @param disableLazy whether to disable lazy loading of validation rules. When validation is
   *     performed, a message's rules will be looked up in a cache. If they are not found, by
   *     default they will be processed and lazily-loaded into the cache. Setting this to false will
   *     not attempt to lazily-load descriptor information not found in the cache and essentially
   *     makes the entire cache read-only, eliminating thread contention and race conditions.
   * @return An eager builder instance.
   */
  public static EagerBuilder newBuilder(List<Descriptor> descriptors, boolean disableLazy) {
    return new EagerBuilder(descriptors, disableLazy);
  }
}

abstract class BaseBuilder<T extends BaseBuilder<T>> {
  @Nullable protected Config config;

  public T withConfig(Config config) {
    this.config = config;
    return self();
  }

  abstract T self();
}
