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

import build.buf.validate.FieldRules;
import build.buf.validate.TimestampRules;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Timestamp;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Native evaluator for the fixed-bound {@code timestamp} rules: {@code const}, {@code lt}, {@code
 * lte}, {@code gt}, and {@code gte}. The now-relative rules ({@code lt_now}, {@code gt_now}, {@code
 * within}) depend on the shared per-validate {@code now} variable and stay on the CEL residual.
 *
 * <p>Range semantics mirror {@link NumericRulesEvaluator}, including the exclusive-range reading
 * where {@code gt > lt} means "outside {@code [lt, gt]}" and the combined rule ids ({@code
 * timestamp.gt_lt}, {@code timestamp.gt_lt_exclusive}, ...).
 */
final class TimestampRulesEvaluator implements Evaluator {
  private static final FieldDescriptor TIMESTAMP_RULES_DESC =
      FieldRules.getDescriptor().findFieldByNumber(FieldRules.TIMESTAMP_FIELD_NUMBER);
  private static final RuleSite CONST_SITE =
      RuleSite.of(
          TIMESTAMP_RULES_DESC,
          TimestampRules.getDescriptor().findFieldByNumber(TimestampRules.CONST_FIELD_NUMBER),
          "timestamp.const",
          null);
  private static final RuleSite LT_SITE =
      RuleSite.of(
          TIMESTAMP_RULES_DESC,
          TimestampRules.getDescriptor().findFieldByNumber(TimestampRules.LT_FIELD_NUMBER),
          null,
          null);
  private static final RuleSite LTE_SITE =
      RuleSite.of(
          TIMESTAMP_RULES_DESC,
          TimestampRules.getDescriptor().findFieldByNumber(TimestampRules.LTE_FIELD_NUMBER),
          null,
          null);
  private static final RuleSite GT_SITE =
      RuleSite.of(
          TIMESTAMP_RULES_DESC,
          TimestampRules.getDescriptor().findFieldByNumber(TimestampRules.GT_FIELD_NUMBER),
          null,
          null);
  private static final RuleSite GTE_SITE =
      RuleSite.of(
          TIMESTAMP_RULES_DESC,
          TimestampRules.getDescriptor().findFieldByNumber(TimestampRules.GTE_FIELD_NUMBER),
          null,
          null);

  private final RuleBase base;
  private final @Nullable Instant constVal;
  private final @Nullable Timestamp constProto;
  private final @Nullable Instant loVal;
  private final @Nullable Timestamp loProto;
  private final TemporalBounds.LowerBound lowerKind;
  private final @Nullable Instant hiVal;
  private final @Nullable Timestamp hiProto;
  private final TemporalBounds.UpperBound upperKind;

  private TimestampRulesEvaluator(
      RuleBase base,
      @Nullable Instant constVal,
      @Nullable Timestamp constProto,
      @Nullable Instant loVal,
      @Nullable Timestamp loProto,
      TemporalBounds.LowerBound lowerKind,
      @Nullable Instant hiVal,
      @Nullable Timestamp hiProto,
      TemporalBounds.UpperBound upperKind) {
    this.base = base;
    this.constVal = constVal;
    this.constProto = constProto;
    this.loVal = loVal;
    this.loProto = loProto;
    this.lowerKind = lowerKind;
    this.hiVal = hiVal;
    this.hiProto = hiProto;
    this.upperKind = upperKind;
  }

  /**
   * Attempts to build a {@link TimestampRulesEvaluator} for the timestamp sub-rules on the given
   * builder. Covered rules are cleared on the builder; {@code lt_now}, {@code gt_now}, and {@code
   * within} are left for CEL. Returns null when no covered rule is set or unknown fields are
   * present.
   */
  static @Nullable Evaluator tryBuild(RuleBase base, FieldRules.Builder rulesBuilder) {
    if (!rulesBuilder.hasTimestamp()) {
      return null;
    }
    TimestampRules rules = rulesBuilder.getTimestamp();
    if (!rules.getUnknownFields().isEmpty()) {
      return null;
    }
    TimestampRules.Builder residual = rules.toBuilder();
    boolean hasRule = false;

    Instant loVal = null;
    Timestamp loProto = null;
    TemporalBounds.LowerBound lowerKind = TemporalBounds.LowerBound.NONE;
    if (rules.hasGt()) {
      lowerKind = TemporalBounds.LowerBound.GT;
      loProto = rules.getGt();
      loVal = TemporalBounds.toInstant(loProto);
      residual.clearGt();
      hasRule = true;
    } else if (rules.hasGte()) {
      lowerKind = TemporalBounds.LowerBound.GTE;
      loProto = rules.getGte();
      loVal = TemporalBounds.toInstant(loProto);
      residual.clearGte();
      hasRule = true;
    }

    Instant hiVal = null;
    Timestamp hiProto = null;
    TemporalBounds.UpperBound upperKind = TemporalBounds.UpperBound.NONE;
    if (rules.hasLt()) {
      upperKind = TemporalBounds.UpperBound.LT;
      hiProto = rules.getLt();
      hiVal = TemporalBounds.toInstant(hiProto);
      residual.clearLt();
      hasRule = true;
    } else if (rules.hasLte()) {
      upperKind = TemporalBounds.UpperBound.LTE;
      hiProto = rules.getLte();
      hiVal = TemporalBounds.toInstant(hiProto);
      residual.clearLte();
      hasRule = true;
    }

    Instant constVal = null;
    Timestamp constProto = null;
    if (rules.hasConst()) {
      constProto = rules.getConst();
      constVal = TemporalBounds.toInstant(constProto);
      residual.clearConst();
      hasRule = true;
    }

    if (!hasRule) {
      return null;
    }
    rulesBuilder.setTimestamp(residual);
    return new TimestampRulesEvaluator(
        base, constVal, constProto, loVal, loProto, lowerKind, hiVal, hiProto, upperKind);
  }

  @Override
  public boolean tautology() {
    return false;
  }

  @Override
  public List<RuleViolation.Builder> evaluate(Value val, boolean failFast) {
    Instant actual = TemporalBounds.toInstant(val.rawValue());
    List<RuleViolation.Builder> violations = null;

    if (constVal != null && !actual.equals(constVal)) {
      violations =
          RuleBase.add(
              violations,
              NativeViolations.newViolation(
                  CONST_SITE,
                  null,
                  "must equal " + TemporalBounds.formatTimestamp(constVal),
                  val,
                  constProto));
      if (failFast) {
        return base.done(violations);
      }
    }

    if (lowerKind != TemporalBounds.LowerBound.NONE
        || upperKind != TemporalBounds.UpperBound.NONE) {
      RuleViolation.Builder rangeViolation =
          TemporalBounds.buildRangeViolation(
              actual,
              loVal,
              lowerKind,
              hiVal,
              upperKind,
              "timestamp",
              TemporalBounds::formatTimestamp,
              lowerKind == TemporalBounds.LowerBound.GT ? GT_SITE : GTE_SITE,
              upperKind == TemporalBounds.UpperBound.LT ? LT_SITE : LTE_SITE,
              val,
              loProto,
              hiProto);
      if (rangeViolation != null) {
        violations = RuleBase.add(violations, rangeViolation);
        if (failFast) {
          return base.done(violations);
        }
      }
    }

    return base.done(violations);
  }
}
