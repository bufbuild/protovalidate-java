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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.protobuf.Duration;
import org.junit.jupiter.api.Test;
import org.projectnessie.cel.common.types.DoubleT;
import org.projectnessie.cel.common.types.Err.ErrException;
import org.projectnessie.cel.common.types.ListT;
import org.projectnessie.cel.common.types.StringT;
import org.projectnessie.cel.common.types.UintT;
import org.projectnessie.cel.common.types.pb.DefaultTypeAdapter;
import org.projectnessie.cel.common.types.ref.Val;

class FormatTest {
  @Test
  void testNotEnoughArgumentsThrows() {
    StringT one = StringT.stringOf("one");
    ListT val = (ListT) ListT.newValArrayList(null, new Val[] {one});

    assertThatThrownBy(
            () -> {
              Format.format("first value: %s and  %s", val);
            })
        .isInstanceOf(ErrException.class)
        .hasMessageContaining("format: not enough arguments");
  }

  @Test
  void testDouble() {
    ListT val =
        (ListT)
            ListT.newValArrayList(
                null,
                new Val[] {
                  DoubleT.doubleOf(-1.20000000000),
                  DoubleT.doubleOf(-1.2),
                  DoubleT.doubleOf(-1.230),
                  DoubleT.doubleOf(-1.002),
                  DoubleT.doubleOf(-0.1),
                  DoubleT.doubleOf(-.1),
                  DoubleT.doubleOf(-1),
                  DoubleT.doubleOf(-0.0),
                  DoubleT.doubleOf(0),
                  DoubleT.doubleOf(0.0),
                  DoubleT.doubleOf(1),
                  DoubleT.doubleOf(0.1),
                  DoubleT.doubleOf(.1),
                  DoubleT.doubleOf(1.002),
                  DoubleT.doubleOf(1.230),
                  DoubleT.doubleOf(1.20000000000)
                });
    String formatted =
        Format.format("%d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d", val);
    assertThat(formatted)
        .isEqualTo(
            "-1.2, -1.2, -1.23, -1.002, -0.1, -0.1, -1, -0, 0, 0, 1, 0.1, 0.1, 1.002, 1.23, 1.2");
  }

  @Test
  void testLargeDecimalValuesAreProperlyFormatted() {
    UintT largeDecimal = UintT.uintOf(999999999999L);
    ListT val = (ListT) ListT.newValArrayList(null, new Val[] {largeDecimal});
    String formatted = Format.format("%s", val);
    assertThat(formatted).isEqualTo("999999999999");
  }

  @Test
  void testDuration() {
    Duration duration = Duration.newBuilder().setSeconds(123).setNanos(45678).build();

    ListT val =
        (ListT) ListT.newGenericArrayList(DefaultTypeAdapter.Instance, new Duration[] {duration});
    String formatted = Format.format("%s", val);
    assertThat(formatted).isEqualTo("123.000045678s");
  }

  @Test
  void testEmptyDuration() {
    Duration duration = Duration.newBuilder().build();
    ListT val =
        (ListT) ListT.newGenericArrayList(DefaultTypeAdapter.Instance, new Duration[] {duration});
    String formatted = Format.format("%s", val);
    assertThat(formatted).isEqualTo("0s");
  }

  @Test
  void testDurationSecondsOnly() {
    Duration duration = Duration.newBuilder().setSeconds(123).build();

    ListT val =
        (ListT) ListT.newGenericArrayList(DefaultTypeAdapter.Instance, new Duration[] {duration});
    String formatted = Format.format("%s", val);
    assertThat(formatted).isEqualTo("123s");
  }

  @Test
  void testDurationNanosOnly() {
    Duration duration = Duration.newBuilder().setNanos(42).build();

    ListT val =
        (ListT) ListT.newGenericArrayList(DefaultTypeAdapter.Instance, new Duration[] {duration});
    String formatted = Format.format("%s", val);
    assertThat(formatted).isEqualTo("0.000000042s");
  }
}
