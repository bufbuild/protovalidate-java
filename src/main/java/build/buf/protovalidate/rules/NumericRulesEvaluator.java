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

package build.buf.protovalidate.rules;

import build.buf.protovalidate.Evaluator;
import build.buf.protovalidate.FieldPathUtils;
import build.buf.protovalidate.RuleViolation;
import build.buf.protovalidate.Value;
import build.buf.validate.FieldRules;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Native evaluator for the standard numeric rules ({@code gt}, {@code gte}, {@code lt}, {@code
 * lte}, {@code const}, {@code in}, {@code not_in}, plus {@code finite} for float/double). Mirrors
 * {@code nativeNumericCompare} in protovalidate-go's {@code native_numeric.go}, parameterized over
 * the boxed Java numeric type ({@code Integer}, {@code Long}, {@code Float}, {@code Double}).
 *
 * <p>Signed vs unsigned semantics are encoded in the supplied {@link NumericTypeConfig}: the
 * config's comparator decides ordering and its formatter decides how values render in messages. The
 * same {@code Integer}-typed evaluator is shared between {@code int32} (signed comparator, {@code
 * String.valueOf} formatter) and {@code uint32} ({@code Integer::compareUnsigned}, {@code
 * Integer::toUnsignedString}).
 */
final class NumericRulesEvaluator<T extends Number & Comparable<T>> implements Evaluator {

  /** Lower bound active on this evaluator. */
  enum LowerBound {
    NONE,
    GTE, // inclusive
    GT // exclusive
  }

  /** Upper bound active on this evaluator. */
  enum UpperBound {
    NONE,
    LT,
    LTE
  }

  private final RuleBase base;
  private final NumericTypeConfig<T> config;
  private final @Nullable T constVal;
  private final List<T> inVals;
  private final List<T> notInVals;
  private final @Nullable T loVal;
  private final LowerBound lowerKind;
  private final @Nullable T hiVal;
  private final UpperBound upperKind;
  private final boolean finite;

  private NumericRulesEvaluator(
      RuleBase base,
      NumericTypeConfig<T> config,
      @Nullable T constVal,
      List<T> inVals,
      List<T> notInVals,
      @Nullable T loVal,
      LowerBound lowerKind,
      @Nullable T hiVal,
      UpperBound upperKind,
      boolean finite) {
    this.base = base;
    this.config = config;
    this.constVal = constVal;
    this.inVals = inVals;
    this.notInVals = notInVals;
    this.loVal = loVal;
    this.lowerKind = lowerKind;
    this.hiVal = hiVal;
    this.upperKind = upperKind;
    this.finite = finite;
  }

  /**
   * Attempts to build a {@link NumericRulesEvaluator} for the rules under {@code rulesField} on
   * {@code rulesBuilder}. Returns null when the typed sub-message is unset, has unknown fields, or
   * carries no rule we cover. On success, clears the covered fields on the builder so CEL doesn't
   * also compile programs for them.
   *
   * @param rulesField the {@code FieldRules} oneof field for this kind (e.g. the {@code int32}
   *     field on {@code FieldRules})
   */
  static <T extends Number & Comparable<T>> @Nullable Evaluator tryBuild(
      RuleBase base, FieldRules.Builder rulesBuilder, NumericTypeConfig<T> config) {
    FieldDescriptor rulesField = config.rulesField;
    if (!rulesBuilder.hasField(rulesField)) {
      return null;
    }
    Message rulesMsg = (Message) rulesBuilder.getField(rulesField);
    if (!rulesMsg.getUnknownFields().isEmpty()) {
      return null;
    }

    NumericDescriptors descs = config.descriptors;
    Message.Builder typedBuilder = rulesMsg.toBuilder();
    boolean hasRule = false;

    T loVal = null;
    LowerBound lowerKind = LowerBound.NONE;
    if (rulesMsg.hasField(descs.gtField)) {
      lowerKind = LowerBound.GT;
      loVal = config.valueClass.cast(rulesMsg.getField(descs.gtField));
      typedBuilder.clearField(descs.gtField);
      hasRule = true;
    } else if (rulesMsg.hasField(descs.gteField)) {
      lowerKind = LowerBound.GTE;
      loVal = config.valueClass.cast(rulesMsg.getField(descs.gteField));
      typedBuilder.clearField(descs.gteField);
      hasRule = true;
    }

    T hiVal = null;
    UpperBound upperKind = UpperBound.NONE;
    if (rulesMsg.hasField(descs.ltField)) {
      upperKind = UpperBound.LT;
      hiVal = config.valueClass.cast(rulesMsg.getField(descs.ltField));
      typedBuilder.clearField(descs.ltField);
      hasRule = true;
    } else if (rulesMsg.hasField(descs.lteField)) {
      upperKind = UpperBound.LTE;
      hiVal = config.valueClass.cast(rulesMsg.getField(descs.lteField));
      typedBuilder.clearField(descs.lteField);
      hasRule = true;
    }

    T constVal = null;
    if (rulesMsg.hasField(descs.constField)) {
      constVal = config.valueClass.cast(rulesMsg.getField(descs.constField));
      typedBuilder.clearField(descs.constField);
      hasRule = true;
    }

    @SuppressWarnings("unchecked")
    List<T> rawInVals = (List<T>) rulesMsg.getField(descs.inField);
    List<T> inVals = rawInVals.isEmpty() ? Collections.emptyList() : rawInVals;
    if (!inVals.isEmpty()) {
      typedBuilder.clearField(descs.inField);
      hasRule = true;
    }

    @SuppressWarnings("unchecked")
    List<T> rawNotInVals = (List<T>) rulesMsg.getField(descs.notInField);
    List<T> notInVals = rawNotInVals.isEmpty() ? Collections.emptyList() : rawNotInVals;
    if (!notInVals.isEmpty()) {
      typedBuilder.clearField(descs.notInField);
      hasRule = true;
    }

    boolean finite = false;
    if (descs.finiteField != null && rulesMsg.hasField(descs.finiteField)) {
      finite = (Boolean) rulesMsg.getField(descs.finiteField);
      typedBuilder.clearField(descs.finiteField);
      hasRule = true;
    }

    if (!hasRule) {
      return null;
    }
    rulesBuilder.setField(rulesField, typedBuilder.build());
    return new NumericRulesEvaluator<T>(
        base, config, constVal, inVals, notInVals, loVal, lowerKind, hiVal, upperKind, finite);
  }

  @Override
  public boolean tautology() {
    return false;
  }

  @Override
  public List<RuleViolation.Builder> evaluate(Value val, boolean failFast) {
    T actual = config.valueClass.cast(val.rawValue());
    List<RuleViolation.Builder> violations = null;

    if (constVal != null && config.comparator.compare(actual, constVal) != 0) {
      violations =
          add(
              violations,
              NativeViolations.newViolation(
                  config.descriptors.constSite,
                  null,
                  "must equal " + config.formatter.apply(constVal),
                  val,
                  constVal));
      if (failFast) {
        return done(violations);
      }
    }

    if (!inVals.isEmpty() && !containsValue(inVals, actual)) {
      violations =
          add(
              violations,
              NativeViolations.newViolation(
                  config.descriptors.inSite,
                  null,
                  "must be in list " + formatList(inVals),
                  val,
                  actual));
      if (failFast) {
        return done(violations);
      }
    }

    if (!notInVals.isEmpty() && containsValue(notInVals, actual)) {
      violations =
          add(
              violations,
              NativeViolations.newViolation(
                  config.descriptors.notInSite,
                  null,
                  "must not be in list " + formatList(notInVals),
                  val,
                  actual));
      if (failFast) {
        return done(violations);
      }
    }

    if (finite && !isFinite(actual)) {
      // descriptors.finiteSite is non-null whenever finite==true (set up at builder time).
      RuleSite site =
          Objects.requireNonNull(
              config.descriptors.finiteSite, "finiteSite must be set when finite is true");
      violations =
          add(violations, NativeViolations.newViolation(site, null, null, val, actual));
      if (failFast) {
        return done(violations);
      }
    }

    if (lowerKind != LowerBound.NONE || upperKind != UpperBound.NONE) {
      RuleViolation.Builder rangeViolation = buildRangeViolation(val, actual);
      if (rangeViolation != null) {
        violations = add(violations, rangeViolation);
        if (failFast) {
          return done(violations);
        }
      }
    }

    return done(violations);
  }

  // --- Per-rule violation builders ---

  /**
   * Builds a violation for the lower/upper bound check, or returns null if the value is in range.
   * Mirrors {@code nativeNumericCompare.evaluateRange} in protovalidate-go, including the
   * exclusive-range semantics where {@code gt > lt} (or equivalents) means "value not in [lt, gt]".
   */
  private RuleViolation.@Nullable Builder buildRangeViolation(Value val, T actual) {
    boolean isNaN = config.nanFailsRange && isNaN(actual);
    if (lowerKind == LowerBound.NONE) {
      if (isNaN || aboveHi(actual)) {
        return NativeViolations.newViolation(
            hiSite(), gtltRule(), "must be " + hiMessage(), val, hiVal);
      }
      return null;
    }
    if (upperKind == UpperBound.NONE) {
      if (isNaN || belowLo(actual)) {
        return NativeViolations.newViolation(
            loSite(), gtltRule(), "must be " + loMessage(), val, loVal);
      }
      return null;
    }
    boolean failure;
    if (isNormalRange()) {
      failure = isNaN || aboveHi(actual) || belowLo(actual);
    } else {
      failure = isNaN || (aboveHi(actual) && belowLo(actual));
    }
    if (failure) {
      String message = "must be " + loMessage() + " " + conjunction() + " " + hiMessage();
      return NativeViolations.newViolation(loSite(), gtltRule(), message, val, loVal);
    }
    return null;
  }

  // --- Comparison helpers (depend on the comparator from config) ---

  private boolean belowLo(T value) {
    int cmp = config.comparator.compare(value, loVal);
    return lowerKind == LowerBound.GT ? cmp <= 0 : cmp < 0;
  }

  private boolean aboveHi(T value) {
    int cmp = config.comparator.compare(value, hiVal);
    return upperKind == UpperBound.LT ? cmp >= 0 : cmp > 0;
  }

  private boolean isNormalRange() {
    // hi >= lo means a normal range. For unsigned kinds this uses the unsigned comparator.
    return config.comparator.compare(hiVal, loVal) >= 0;
  }

  private boolean containsValue(List<T> list, T value) {
    // Use the comparator for equality so unsigned/signed semantics agree. Java's List.contains
    // would use Object.equals, which is fine for boxed primitives but we keep a single source of
    // truth.
    for (T t : list) {
      if (config.comparator.compare(t, value) == 0) {
        return true;
      }
    }
    return false;
  }

  private static <T extends Number> boolean isFinite(T value) {
    if (value instanceof Float) {
      return Float.isFinite(value.floatValue());
    }
    if (value instanceof Double) {
      return Double.isFinite(value.doubleValue());
    }
    // Integer kinds are always finite.
    return true;
  }

  private static <T extends Number> boolean isNaN(T value) {
    if (value instanceof Float) {
      return Float.isNaN(value.floatValue());
    }
    if (value instanceof Double) {
      return Double.isNaN(value.doubleValue());
    }
    return false;
  }

  // --- Rule-id and message helpers (mirror Go's gtltRule / loMessage / hiMessage / conjunction)
  // ---

  private RuleSite loSite() {
    return lowerKind == LowerBound.GT ? config.descriptors.gtSite : config.descriptors.gteSite;
  }

  private RuleSite hiSite() {
    return upperKind == UpperBound.LT ? config.descriptors.ltSite : config.descriptors.lteSite;
  }

  private String gtRulePrefix() {
    return lowerKind == LowerBound.GT ? config.typeName + ".gt" : config.typeName + ".gte";
  }

  private String ltRulePrefix() {
    return upperKind == UpperBound.LT ? config.typeName + ".lt" : config.typeName + ".lte";
  }

  /** Combined rule id, e.g. {@code int32.gt_lt_exclusive}. Mirrors Go's {@code gtltRule}. */
  private String gtltRule() {
    if (lowerKind == LowerBound.NONE) {
      return ltRulePrefix();
    }
    String prefix = gtRulePrefix();
    if (upperKind == UpperBound.LT) {
      prefix += "_lt";
      if (!isNormalRange()) {
        prefix += "_exclusive";
      }
    } else if (upperKind == UpperBound.LTE) {
      prefix += "_lte";
      if (!isNormalRange()) {
        prefix += "_exclusive";
      }
    }
    return prefix;
  }

  private String loMessage() {
    String formatted = config.formatter.apply(loVal);
    return lowerKind == LowerBound.GT
        ? "greater than " + formatted
        : "greater than or equal to " + formatted;
  }

  private String hiMessage() {
    String formatted = config.formatter.apply(hiVal);
    return upperKind == UpperBound.LT
        ? "less than " + formatted
        : "less than or equal to " + formatted;
  }

  private String conjunction() {
    return isNormalRange() ? "and" : "or";
  }

  private String formatList(List<T> vals) {
    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < vals.size(); i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(config.formatter.apply(vals.get(i)));
    }
    sb.append("]");
    return sb.toString();
  }

  // --- Violation list bookkeeping ---

  private static List<RuleViolation.Builder> add(
      @Nullable List<RuleViolation.Builder> violations, RuleViolation.Builder v) {
    if (violations == null) {
      violations = new ArrayList<>(2);
    }
    violations.add(v);
    return violations;
  }

  private List<RuleViolation.Builder> done(@Nullable List<RuleViolation.Builder> violations) {
    if (violations == null || violations.isEmpty()) {
      return RuleViolation.NO_VIOLATIONS;
    }
    return FieldPathUtils.updatePaths(
        violations, base.getFieldPathElement(), base.getRulePrefixElements());
  }
}
