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

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Message;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/**
 * Shared machinery for the temporal native evaluators ({@link TimestampRulesEvaluator}, {@link
 * DurationRulesEvaluator}): proto-to-{@code java.time} conversion, CEL-compatible value formatting
 * for violation messages, and the bound/range logic that mirrors {@link NumericRulesEvaluator}'s
 * comparisons — including the exclusive-range reading where a lower bound above the upper bound
 * means "outside {@code [hi, lo]}".
 */
final class TemporalBounds {
  private TemporalBounds() {}

  /** Lower bound active on a temporal evaluator. */
  enum LowerBound {
    NONE,
    GTE, // inclusive
    GT // exclusive
  }

  /** Upper bound active on a temporal evaluator. */
  enum UpperBound {
    NONE,
    LT,
    LTE
  }

  /**
   * Converts a raw {@code google.protobuf.Timestamp} field value to an {@link Instant}. The value
   * is the generated {@link com.google.protobuf.Timestamp} for generated messages and a {@link
   * Message} view for dynamic ones; both carry {@code seconds}/{@code nanos} at field numbers 1/2.
   */
  static Instant toInstant(Object raw) {
    if (raw instanceof com.google.protobuf.Timestamp) {
      com.google.protobuf.Timestamp ts = (com.google.protobuf.Timestamp) raw;
      return Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos());
    }
    long[] parts = secondsNanos((Message) raw);
    return Instant.ofEpochSecond(parts[0], parts[1]);
  }

  /** Converts a raw {@code google.protobuf.Duration} field value to a {@link Duration}. */
  static Duration toDuration(Object raw) {
    if (raw instanceof com.google.protobuf.Duration) {
      com.google.protobuf.Duration d = (com.google.protobuf.Duration) raw;
      return Duration.ofSeconds(d.getSeconds(), d.getNanos());
    }
    long[] parts = secondsNanos((Message) raw);
    return Duration.ofSeconds(parts[0], parts[1]);
  }

  private static long[] secondsNanos(Message msg) {
    Descriptor descriptor = msg.getDescriptorForType();
    long seconds = (Long) msg.getField(descriptor.findFieldByNumber(1));
    int nanos = (Integer) msg.getField(descriptor.findFieldByNumber(2));
    return new long[] {seconds, nanos};
  }

  /**
   * Formats an {@link Instant} the way CEL's {@code %s} renders a timestamp in the rule messages:
   * RFC 3339 UTC with a {@code Z} suffix, fractional seconds only when non-zero.
   */
  static String formatTimestamp(Instant value) {
    return value.toString();
  }

  /**
   * Formats a {@link Duration} the way CEL's {@code %s} renders a duration in the rule messages:
   * signed decimal seconds with trailing zeros trimmed, suffixed {@code s} (e.g. {@code 30s},
   * {@code 60.5s}).
   */
  static String formatDuration(Duration value) {
    long seconds = value.getSeconds();
    int nanos = value.getNano();
    if (nanos == 0) {
      return seconds + "s";
    }
    // Duration normalizes to nanos in [0, 1e9); negative sub-second durations carry seconds=-1.
    StringBuilder sb = new StringBuilder();
    if (seconds < 0 || (seconds == 0 && value.isNegative())) {
      sb.append('-');
      seconds = -seconds - 1;
      nanos = 1_000_000_000 - nanos;
    }
    String frac = String.format("%09d", nanos);
    int end = frac.length();
    while (end > 0 && frac.charAt(end - 1) == '0') {
      end--;
    }
    return sb.append(seconds).append('.').append(frac, 0, end).append('s').toString();
  }

  /**
   * Builds a violation for the lower/upper bound check, or returns null when the value is in range.
   * Mirrors {@link NumericRulesEvaluator}'s range logic (sans NaN handling): both bounds present
   * with {@code hi < lo} is the exclusive range, failing only values inside {@code [hi, lo]}.
   *
   * @param actual the field's value in {@code java.time} form
   * @param typeName the rule-id prefix ({@code "timestamp"} or {@code "duration"})
   * @param formatter renders bound values in messages, CEL-compatibly
   * @param loSite the {@code gt}/{@code gte} rule site matching {@code lowerKind}
   * @param hiSite the {@code lt}/{@code lte} rule site matching {@code upperKind}
   * @param loRuleValue the lower bound's proto value for the violation's rule_value
   * @param hiRuleValue the upper bound's proto value for the violation's rule_value
   */
  static <T extends Comparable<T>> RuleViolation.@Nullable Builder buildRangeViolation(
      T actual,
      @Nullable T loVal,
      LowerBound lowerKind,
      @Nullable T hiVal,
      UpperBound upperKind,
      String typeName,
      Function<T, String> formatter,
      RuleSite loSite,
      RuleSite hiSite,
      Value val,
      @Nullable Object loRuleValue,
      @Nullable Object hiRuleValue) {
    // A bound value is non-null exactly when its kind is not NONE; the requireNonNull calls
    // materialize that invariant for NullAway.
    if (lowerKind == LowerBound.NONE) {
      T hi = java.util.Objects.requireNonNull(hiVal, "hiVal");
      if (aboveHi(actual, hi, upperKind)) {
        String ruleId = typeName + (upperKind == UpperBound.LT ? ".lt" : ".lte");
        return NativeViolations.newViolation(
            hiSite, ruleId, "must be " + hiMessage(hi, upperKind, formatter), val, hiRuleValue);
      }
      return null;
    }
    T lo = java.util.Objects.requireNonNull(loVal, "loVal");
    if (upperKind == UpperBound.NONE) {
      if (belowLo(actual, lo, lowerKind)) {
        String ruleId = typeName + (lowerKind == LowerBound.GT ? ".gt" : ".gte");
        return NativeViolations.newViolation(
            loSite, ruleId, "must be " + loMessage(lo, lowerKind, formatter), val, loRuleValue);
      }
      return null;
    }
    T hi = java.util.Objects.requireNonNull(hiVal, "hiVal");
    boolean normalRange = hi.compareTo(lo) >= 0;
    boolean failure;
    if (normalRange) {
      failure = aboveHi(actual, hi, upperKind) || belowLo(actual, lo, lowerKind);
    } else {
      failure = aboveHi(actual, hi, upperKind) && belowLo(actual, lo, lowerKind);
    }
    if (!failure) {
      return null;
    }
    StringBuilder ruleId =
        new StringBuilder(typeName)
            .append(lowerKind == LowerBound.GT ? ".gt" : ".gte")
            .append(upperKind == UpperBound.LT ? "_lt" : "_lte");
    if (!normalRange) {
      ruleId.append("_exclusive");
    }
    String message =
        "must be "
            + loMessage(lo, lowerKind, formatter)
            + (normalRange ? " and " : " or ")
            + hiMessage(hi, upperKind, formatter);
    return NativeViolations.newViolation(loSite, ruleId.toString(), message, val, loRuleValue);
  }

  private static <T extends Comparable<T>> boolean belowLo(T value, T loVal, LowerBound kind) {
    int cmp = value.compareTo(loVal);
    return kind == LowerBound.GT ? cmp <= 0 : cmp < 0;
  }

  private static <T extends Comparable<T>> boolean aboveHi(T value, T hiVal, UpperBound kind) {
    int cmp = value.compareTo(hiVal);
    return kind == UpperBound.LT ? cmp >= 0 : cmp > 0;
  }

  private static <T> String loMessage(T loVal, LowerBound kind, Function<T, String> formatter) {
    String formatted = formatter.apply(loVal);
    return kind == LowerBound.GT
        ? "greater than " + formatted
        : "greater than or equal to " + formatted;
  }

  private static <T> String hiMessage(T hiVal, UpperBound kind, Function<T, String> formatter) {
    String formatted = formatter.apply(hiVal);
    return kind == UpperBound.LT ? "less than " + formatted : "less than or equal to " + formatted;
  }
}
