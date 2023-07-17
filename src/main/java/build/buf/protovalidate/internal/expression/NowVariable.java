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

package build.buf.protovalidate.internal.expression;

import java.time.Instant;
import org.projectnessie.cel.common.types.TimestampT;
import org.projectnessie.cel.interpreter.Activation;
import org.projectnessie.cel.interpreter.ResolvedValue;

/**
 * {@link NowVariable} implements {@link Activation}, providing a lazily produced timestamp for
 * accessing the variable `now` that's constant within an evaluation.
 */
public class NowVariable implements Activation {
  private static final String NOW_NAME = "now";

  private ResolvedValue resolvedValue;

  @Override
  public ResolvedValue resolveName(String name) {
    if (!name.equals(NOW_NAME)) {
      return ResolvedValue.ABSENT;
    } else if (resolvedValue != null) {
      return resolvedValue;
    }
    Instant instant = Instant.now(); // UTC.
    TimestampT value = TimestampT.timestampOf(instant);
    resolvedValue = ResolvedValue.resolvedValue(value);
    return resolvedValue;
  }

  @Override
  public Activation parent() {
    return Activation.emptyActivation();
  }
}
