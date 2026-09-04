// Copyright 2023-2026 Buf Technologies, Inc.
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

import build.buf.protovalidate.exceptions.ValidationException;
import build.buf.validate.Violation;
import com.example.noimports.validationtest.DurationConst;
import com.example.noimports.validationtest.DurationGteLte;
import com.example.noimports.validationtest.DurationInNotIn;
import com.example.noimports.validationtest.RepeatedItemInt32Gt;
import com.example.noimports.validationtest.RepeatedItemStringMinLen;
import com.example.noimports.validationtest.RepeatedItemTimestampGt;
import com.example.noimports.validationtest.RepeatedItemWrapperGt;
import com.example.noimports.validationtest.TimestampConst;
import com.example.noimports.validationtest.TimestampGtLt;
import com.example.noimports.validationtest.TimestampGtLtExclusive;
import com.example.noimports.validationtest.TimestampGteLtNow;
import com.example.noimports.validationtest.TimestampWithin;
import com.google.protobuf.Duration;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Asserts that the native evaluators produce violations identical to the CEL evaluators for the
 * repeated-item and timestamp/duration rule fixtures: same verdict, same {@link Violation} protos
 * (rule id, message, field path, rule path, values), in the same order.
 */
class NativeCelParityTest {

  private static final Validator NATIVE =
      ValidatorFactory.newBuilder()
          .withConfig(Config.newBuilder().setEnableNativeRules(true).build())
          .build();
  private static final Validator CEL =
      ValidatorFactory.newBuilder()
          .withConfig(Config.newBuilder().setEnableNativeRules(false).build())
          .build();

  private static void assertParity(Message msg) throws ValidationException {
    ValidationResult nativeResult = NATIVE.validate(msg);
    ValidationResult celResult = CEL.validate(msg);
    List<Violation> nativeProtos =
        nativeResult.getViolations().stream()
            .map(build.buf.protovalidate.Violation::toProto)
            .collect(Collectors.toList());
    List<Violation> celProtos =
        celResult.getViolations().stream()
            .map(build.buf.protovalidate.Violation::toProto)
            .collect(Collectors.toList());
    assertThat(nativeProtos)
        .as("native violations must match CEL violations for %s", msg.getDescriptorForType())
        .containsExactlyElementsOf(celProtos);
  }

  private static Timestamp ts(long seconds) {
    return Timestamp.newBuilder().setSeconds(seconds).build();
  }

  private static Duration dur(long seconds, int nanos) {
    return Duration.newBuilder().setSeconds(seconds).setNanos(nanos).build();
  }

  @Test
  void repeatedItemInt32() throws ValidationException {
    assertParity(RepeatedItemInt32Gt.newBuilder().addVal(1).addVal(2).build());
    assertParity(RepeatedItemInt32Gt.newBuilder().addVal(1).addVal(0).addVal(-2).build());
    assertParity(RepeatedItemInt32Gt.getDefaultInstance());
  }

  @Test
  void repeatedItemString() throws ValidationException {
    assertParity(RepeatedItemStringMinLen.newBuilder().addVal("ab").addVal("abc").build());
    assertParity(RepeatedItemStringMinLen.newBuilder().addVal("ab").addVal("x").addVal("").build());
  }

  @Test
  void repeatedItemWrapper() throws ValidationException {
    assertParity(RepeatedItemWrapperGt.newBuilder().addVal(Int32Value.of(1)).build());
    assertParity(
        RepeatedItemWrapperGt.newBuilder()
            .addVal(Int32Value.of(1))
            .addVal(Int32Value.of(0))
            .build());
  }

  @Test
  void repeatedItemTimestamp() throws ValidationException {
    assertParity(RepeatedItemTimestampGt.newBuilder().addVal(ts(1500)).build());
    assertParity(RepeatedItemTimestampGt.newBuilder().addVal(ts(1500)).addVal(ts(500)).build());
  }

  @Test
  void timestampRange() throws ValidationException {
    assertParity(TimestampGtLt.newBuilder().setVal(ts(1500)).build());
    assertParity(TimestampGtLt.newBuilder().setVal(ts(500)).build());
    assertParity(TimestampGtLt.newBuilder().setVal(ts(2500)).build());
    // Boundary values: gt/lt are exclusive bounds.
    assertParity(TimestampGtLt.newBuilder().setVal(ts(1000)).build());
    assertParity(TimestampGtLt.newBuilder().setVal(ts(2000)).build());
    // Absent message field: rules are skipped.
    assertParity(TimestampGtLt.getDefaultInstance());
  }

  @Test
  void timestampExclusiveRange() throws ValidationException {
    // gt=2000, lt=1000: valid values are outside [1000, 2000].
    assertParity(TimestampGtLtExclusive.newBuilder().setVal(ts(500)).build());
    assertParity(TimestampGtLtExclusive.newBuilder().setVal(ts(1500)).build());
    assertParity(TimestampGtLtExclusive.newBuilder().setVal(ts(2500)).build());
  }

  @Test
  void timestampConst() throws ValidationException {
    assertParity(TimestampConst.newBuilder().setVal(ts(1500)).build());
    assertParity(
        TimestampConst.newBuilder()
            .setVal(Timestamp.newBuilder().setSeconds(1500).setNanos(1))
            .build());
  }

  @Test
  void timestampNowRelativeResidual() throws ValidationException {
    // gte is native; lt_now stays on the CEL residual. Both fire on the right inputs.
    assertParity(TimestampGteLtNow.newBuilder().setVal(ts(1500)).build());
    assertParity(TimestampGteLtNow.newBuilder().setVal(ts(500)).build());
    // Far future: violates lt_now but not gte.
    assertParity(
        TimestampGteLtNow.newBuilder().setVal(ts(Instant.now().getEpochSecond() + 86_400)).build());
  }

  @Test
  void timestampWithinResidual() throws ValidationException {
    assertParity(TimestampWithin.newBuilder().setVal(ts(Instant.now().getEpochSecond())).build());
    assertParity(TimestampWithin.newBuilder().setVal(ts(1500)).build());
  }

  @Test
  void durationRange() throws ValidationException {
    assertParity(DurationGteLte.newBuilder().setVal(dur(50, 0)).build());
    assertParity(DurationGteLte.newBuilder().setVal(dur(5, 0)).build());
    assertParity(DurationGteLte.newBuilder().setVal(dur(500, 0)).build());
    // Boundary values: gte/lte are inclusive bounds.
    assertParity(DurationGteLte.newBuilder().setVal(dur(10, 0)).build());
    assertParity(DurationGteLte.newBuilder().setVal(dur(100, 0)).build());
    assertParity(DurationGteLte.getDefaultInstance());
  }

  @Test
  void durationConst() throws ValidationException {
    assertParity(DurationConst.newBuilder().setVal(dur(30, 0)).build());
    assertParity(DurationConst.newBuilder().setVal(dur(30, 1)).build());
  }

  @Test
  void durationInNotIn() throws ValidationException {
    assertParity(DurationInNotIn.newBuilder().setVal(dur(1, 0)).build());
    assertParity(DurationInNotIn.newBuilder().setVal(dur(60, 500_000_000)).build());
    assertParity(DurationInNotIn.newBuilder().setVal(dur(2, 0)).build());
    assertParity(DurationInNotIn.newBuilder().setVal(dur(3, 0)).build());
  }
}
