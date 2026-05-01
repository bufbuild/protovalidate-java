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
import build.buf.protovalidate.RuleViolation;
import build.buf.protovalidate.Value;
import build.buf.validate.EnumRules;
import build.buf.validate.FieldRules;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Native evaluator for enum {@code const}/{@code in}/{@code not_in}. The {@code defined_only} rule
 * is handled separately by the existing {@link build.buf.protovalidate.EnumEvaluator}; both can be
 * active simultaneously and the {@link build.buf.protovalidate.ValueEvaluator} runs them in order.
 */
final class EnumRulesEvaluator implements Evaluator {
  private static final FieldDescriptor ENUM_RULES_DESC =
      FieldRules.getDescriptor().findFieldByNumber(FieldRules.ENUM_FIELD_NUMBER);
  private static final FieldDescriptor CONST_DESC =
      EnumRules.getDescriptor().findFieldByNumber(EnumRules.CONST_FIELD_NUMBER);
  private static final FieldDescriptor IN_DESC =
      EnumRules.getDescriptor().findFieldByNumber(EnumRules.IN_FIELD_NUMBER);
  private static final FieldDescriptor NOT_IN_DESC =
      EnumRules.getDescriptor().findFieldByNumber(EnumRules.NOT_IN_FIELD_NUMBER);
  private static final RuleSite CONST_SITE =
      RuleSite.of(ENUM_RULES_DESC, CONST_DESC, "enum.const", null);
  private static final RuleSite IN_SITE = RuleSite.of(ENUM_RULES_DESC, IN_DESC, "enum.in", null);
  private static final RuleSite NOT_IN_SITE =
      RuleSite.of(ENUM_RULES_DESC, NOT_IN_DESC, "enum.not_in", null);

  private final RuleBase base;
  private final @Nullable Integer constVal;
  private final List<Integer> inVals;
  private final List<Integer> notInVals;

  private EnumRulesEvaluator(
      RuleBase base, @Nullable Integer constVal, List<Integer> inVals, List<Integer> notInVals) {
    this.base = base;
    this.constVal = constVal;
    this.inVals = inVals;
    this.notInVals = notInVals;
  }

  /**
   * Builds a {@link EnumRulesEvaluator} for the {@code const}/{@code in}/{@code not_in} rules on
   * the supplied {@code FieldRules.Builder}'s enum sub-message. Returns null when the enum sub-
   * message is unset, has unknown fields, or has none of the covered rules. The {@code
   * defined_only} field is left untouched on the residual so the existing {@link
   * build.buf.protovalidate.EnumEvaluator} continues to handle it.
   */
  static @Nullable Evaluator tryBuild(RuleBase base, FieldRules.Builder rulesBuilder) {
    if (!rulesBuilder.hasEnum()) {
      return null;
    }
    EnumRules enumRules = rulesBuilder.getEnum();
    if (!enumRules.getUnknownFields().isEmpty()) {
      return null;
    }

    EnumRules.Builder eb = enumRules.toBuilder();
    boolean hasRule = false;

    Integer constVal = null;
    if (enumRules.hasConst()) {
      constVal = enumRules.getConst();
      eb.clearConst();
      hasRule = true;
    }

    // Proto returns immutable views; we only read them.
    List<Integer> inVals = enumRules.getInList();
    if (!inVals.isEmpty()) {
      eb.clearIn();
      hasRule = true;
    }

    List<Integer> notInVals = enumRules.getNotInList();
    if (!notInVals.isEmpty()) {
      eb.clearNotIn();
      hasRule = true;
    }

    if (!hasRule) {
      return null;
    }
    rulesBuilder.setEnum(eb.build());
    return new EnumRulesEvaluator(base, constVal, inVals, notInVals);
  }

  @Override
  public boolean tautology() {
    return false;
  }

  @Override
  public List<RuleViolation.Builder> evaluate(Value val, boolean failFast) {
    int actual = enumNumber(val.rawValue());
    List<RuleViolation.Builder> violations = null;

    if (constVal != null && actual != constVal) {
      RuleViolation.Builder b =
          NativeViolations.newViolation(CONST_SITE, null, "must equal " + constVal, val, constVal);
      violations = RuleBase.add(violations, b);
      if (failFast) {
        return base.done(violations);
      }
    }

    if (!inVals.isEmpty() && !inVals.contains(actual)) {
      RuleViolation.Builder b =
          NativeViolations.newViolation(
              IN_SITE, null, "must be in list " + RuleBase.formatList(inVals), val, actual);
      violations = RuleBase.add(violations, b);
      if (failFast) {
        return base.done(violations);
      }
    }

    if (!notInVals.isEmpty() && notInVals.contains(actual)) {
      RuleViolation.Builder b =
          NativeViolations.newViolation(
              NOT_IN_SITE,
              null,
              "must not be in list " + RuleBase.formatList(notInVals),
              val,
              actual);
      violations = RuleBase.add(violations, b);
      if (failFast) {
        return base.done(violations);
      }
    }

    return base.done(violations);
  }

  /**
   * Extracts the enum's numeric value from {@link Value#rawValue()}. Java protobuf normally returns
   * an {@link EnumValueDescriptor}, but unknown enum values may surface as {@link Integer}
   * depending on the proto edition; handle both.
   */
  private static int enumNumber(Object raw) {
    if (raw instanceof EnumValueDescriptor) {
      return ((EnumValueDescriptor) raw).getNumber();
    }
    if (raw instanceof Integer) {
      return (Integer) raw;
    }
    if (raw instanceof Long) {
      return ((Long) raw).intValue();
    }
    throw new IllegalStateException(
        "unexpected enum value representation: " + raw.getClass().getName());
  }
}
