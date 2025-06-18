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

import com.google.protobuf.Timestamp;
import dev.cel.runtime.CelVariableResolver;
import java.time.Instant;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * {@link NowVariable} implements {@link CelVariableResolver}, providing a lazily produced timestamp
 * for accessing the variable `now` that's constant within an evaluation.
 */
final class NowVariable implements CelVariableResolver {
  /** The name of the 'now' variable. */
  static final String NOW_NAME = "now";

  /** The resolved value of the 'now' variable. */
  @Nullable private Timestamp now;

  /** Creates an instance of a "now" variable. */
  NowVariable() {}

  @Override
  public Optional<Object> find(String name) {
    if (!name.equals(NOW_NAME)) {
      return Optional.empty();
    }
    if (this.now == null) {
      Instant nowInstant = Instant.now();
      now =
          Timestamp.newBuilder()
              .setSeconds(nowInstant.getEpochSecond())
              .setNanos(nowInstant.getNano())
              .build();
    }
    return Optional.of(this.now);
  }
}
