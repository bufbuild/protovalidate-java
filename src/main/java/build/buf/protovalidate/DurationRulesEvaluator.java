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

import build.buf.validate.DurationRules;
import build.buf.validate.FieldRules;
import com.google.protobuf.Descriptors.FieldDescriptor;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Native evaluator for the standard {@code duration} rules: {@code const}, {@code lt}, {@code lte},
 * {@code gt}, {@code gte}, {@code in}, and {@code not_in}.
 *
 * <p>Range semantics mirror {@link NumericRulesEvaluator}, including the exclusive-range reading
 * where {@code gt > lt} means "outside {@code [lt, gt]}" and the combined rule ids ({@code
 * duration.gt_lt}, {@code duration.gt_lt_exclusive}, ...).
 */
final class DurationRulesEvaluator implements Evaluator {
  private static final FieldDescriptor DURATION_RULES_DESC =
      FieldRules.getDescriptor().findFieldByNumber(FieldRules.DURATION_FIELD_NUMBER);
  private static final RuleSite CONST_SITE =
      RuleSite.of(
          DURATION_RULES_DESC,
          DurationRules.getDescriptor().findFieldByNumber(DurationRules.CONST_FIELD_NUMBER),
          "duration.const",
          null);
  private static final RuleSite LT_SITE =
      RuleSite.of(
          DURATION_RULES_DESC,
          DurationRules.getDescriptor().findFieldByNumber(DurationRules.LT_FIELD_NUMBER),
          null,
          null);
  private static final RuleSite LTE_SITE =
      RuleSite.of(
          DURATION_RULES_DESC,
          DurationRules.getDescriptor().findFieldByNumber(DurationRules.LTE_FIELD_NUMBER),
          null,
          null);
  private static final RuleSite GT_SITE =
      RuleSite.of(
          DURATION_RULES_DESC,
          DurationRules.getDescriptor().findFieldByNumber(DurationRules.GT_FIELD_NUMBER),
          null,
          null);
  private static final RuleSite GTE_SITE =
      RuleSite.of(
          DURATION_RULES_DESC,
          DurationRules.getDescriptor().findFieldByNumber(DurationRules.GTE_FIELD_NUMBER),
          null,
          null);
  private static final RuleSite IN_SITE =
      RuleSite.of(
          DURATION_RULES_DESC,
          DurationRules.getDescriptor().findFieldByNumber(DurationRules.IN_FIELD_NUMBER),
          "duration.in",
          null);
  private static final RuleSite NOT_IN_SITE =
      RuleSite.of(
          DURATION_RULES_DESC,
          DurationRules.getDescriptor().findFieldByNumber(DurationRules.NOT_IN_FIELD_NUMBER),
          "duration.not_in",
          null);

  private final RuleBase base;
  private final @Nullable Duration constVal;
  private final com.google.protobuf.@Nullable Duration constProto;
  private final List<Duration> inVals;
  private final List<com.google.protobuf.Duration> inProtos;
  private final List<Duration> notInVals;
  private final List<com.google.protobuf.Duration> notInProtos;
  private final @Nullable Duration loVal;
  private final com.google.protobuf.@Nullable Duration loProto;
  private final TemporalBounds.LowerBound lowerKind;
  private final @Nullable Duration hiVal;
  private final com.google.protobuf.@Nullable Duration hiProto;
  private final TemporalBounds.UpperBound upperKind;

  private DurationRulesEvaluator(
      RuleBase base,
      @Nullable Duration constVal,
      com.google.protobuf.@Nullable Duration constProto,
      List<Duration> inVals,
      List<com.google.protobuf.Duration> inProtos,
      List<Duration> notInVals,
      List<com.google.protobuf.Duration> notInProtos,
      @Nullable Duration loVal,
      com.google.protobuf.@Nullable Duration loProto,
      TemporalBounds.LowerBound lowerKind,
      @Nullable Duration hiVal,
      com.google.protobuf.@Nullable Duration hiProto,
      TemporalBounds.UpperBound upperKind) {
    this.base = base;
    this.constVal = constVal;
    this.constProto = constProto;
    this.inVals = inVals;
    this.inProtos = inProtos;
    this.notInVals = notInVals;
    this.notInProtos = notInProtos;
    this.loVal = loVal;
    this.loProto = loProto;
    this.lowerKind = lowerKind;
    this.hiVal = hiVal;
    this.hiProto = hiProto;
    this.upperKind = upperKind;
  }

  /**
   * Attempts to build a {@link DurationRulesEvaluator} for the duration sub-rules on the given
   * builder. Covered rules are cleared on the builder so CEL doesn't recompile them. Returns null
   * when no covered rule is set or unknown fields are present.
   */
  static @Nullable Evaluator tryBuild(RuleBase base, FieldRules.Builder rulesBuilder) {
    if (!rulesBuilder.hasDuration()) {
      return null;
    }
    DurationRules rules = rulesBuilder.getDuration();
    if (!rules.getUnknownFields().isEmpty()) {
      return null;
    }
    DurationRules.Builder residual = rules.toBuilder();
    boolean hasRule = false;

    Duration loVal = null;
    com.google.protobuf.Duration loProto = null;
    TemporalBounds.LowerBound lowerKind = TemporalBounds.LowerBound.NONE;
    if (rules.hasGt()) {
      lowerKind = TemporalBounds.LowerBound.GT;
      loProto = rules.getGt();
      loVal = TemporalBounds.toDuration(loProto);
      residual.clearGt();
      hasRule = true;
    } else if (rules.hasGte()) {
      lowerKind = TemporalBounds.LowerBound.GTE;
      loProto = rules.getGte();
      loVal = TemporalBounds.toDuration(loProto);
      residual.clearGte();
      hasRule = true;
    }

    Duration hiVal = null;
    com.google.protobuf.Duration hiProto = null;
    TemporalBounds.UpperBound upperKind = TemporalBounds.UpperBound.NONE;
    if (rules.hasLt()) {
      upperKind = TemporalBounds.UpperBound.LT;
      hiProto = rules.getLt();
      hiVal = TemporalBounds.toDuration(hiProto);
      residual.clearLt();
      hasRule = true;
    } else if (rules.hasLte()) {
      upperKind = TemporalBounds.UpperBound.LTE;
      hiProto = rules.getLte();
      hiVal = TemporalBounds.toDuration(hiProto);
      residual.clearLte();
      hasRule = true;
    }

    Duration constVal = null;
    com.google.protobuf.Duration constProto = null;
    if (rules.hasConst()) {
      constProto = rules.getConst();
      constVal = TemporalBounds.toDuration(constProto);
      residual.clearConst();
      hasRule = true;
    }

    List<com.google.protobuf.Duration> inProtos = rules.getInList();
    List<Duration> inVals = Collections.emptyList();
    if (!inProtos.isEmpty()) {
      inVals = toDurations(inProtos);
      residual.clearIn();
      hasRule = true;
    }

    List<com.google.protobuf.Duration> notInProtos = rules.getNotInList();
    List<Duration> notInVals = Collections.emptyList();
    if (!notInProtos.isEmpty()) {
      notInVals = toDurations(notInProtos);
      residual.clearNotIn();
      hasRule = true;
    }

    if (!hasRule) {
      return null;
    }
    rulesBuilder.setDuration(residual);
    return new DurationRulesEvaluator(
        base,
        constVal,
        constProto,
        inVals,
        inProtos,
        notInVals,
        notInProtos,
        loVal,
        loProto,
        lowerKind,
        hiVal,
        hiProto,
        upperKind);
  }

  private static List<Duration> toDurations(List<com.google.protobuf.Duration> protos) {
    List<Duration> out = new ArrayList<>(protos.size());
    for (com.google.protobuf.Duration proto : protos) {
      out.add(TemporalBounds.toDuration(proto));
    }
    return out;
  }

  @Override
  public boolean tautology() {
    return false;
  }

  @Override
  public List<RuleViolation.Builder> evaluate(Value val, boolean failFast) {
    Duration actual = TemporalBounds.toDuration(val.rawValue());
    List<RuleViolation.Builder> violations = null;

    if (constVal != null && !actual.equals(constVal)) {
      violations =
          RuleBase.add(
              violations,
              NativeViolations.newViolation(
                  CONST_SITE,
                  null,
                  "must equal " + TemporalBounds.formatDuration(constVal),
                  val,
                  constProto));
      if (failFast) {
        return base.done(violations);
      }
    }

    if (!inVals.isEmpty() && !inVals.contains(actual)) {
      violations =
          RuleBase.add(
              violations,
              NativeViolations.newViolation(
                  IN_SITE, null, "must be in list " + formatList(inVals), val, inProtos));
      if (failFast) {
        return base.done(violations);
      }
    }

    if (!notInVals.isEmpty() && notInVals.contains(actual)) {
      violations =
          RuleBase.add(
              violations,
              NativeViolations.newViolation(
                  NOT_IN_SITE,
                  null,
                  "must not be in list " + formatList(notInVals),
                  val,
                  notInProtos));
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
              "duration",
              TemporalBounds::formatDuration,
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

  private static String formatList(List<Duration> vals) {
    return RuleBase.formatList(vals, TemporalBounds::formatDuration);
  }
}
