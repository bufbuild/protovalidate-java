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

import build.buf.validate.BoolRules;
import build.buf.validate.FieldRules;
import com.google.protobuf.Descriptors.FieldDescriptor;
import java.util.Collections;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Native evaluator for {@code bool} rules. Currently covers {@code bool.const}; the only standard
 * rule defined for bool fields.
 */
final class BoolRulesEvaluator implements Evaluator {
  private static final FieldDescriptor BOOL_RULES_DESC =
      FieldRules.getDescriptor().findFieldByNumber(FieldRules.BOOL_FIELD_NUMBER);
  private static final FieldDescriptor CONST_DESC =
      BoolRules.getDescriptor().findFieldByNumber(BoolRules.CONST_FIELD_NUMBER);
  private static final RuleSite CONST_SITE =
      RuleSite.of(BOOL_RULES_DESC, CONST_DESC, "bool.const", null);

  private final RuleBase base;
  private final boolean expected;

  private BoolRulesEvaluator(RuleBase base, boolean expected) {
    this.base = base;
    this.expected = expected;
  }

  /**
   * Attempts to build a {@link BoolRulesEvaluator} for the bool sub-rules on the given {@code
   * FieldRules.Builder}. Returns null if the rules aren't natively handleable (no bool oneof case
   * set, no covered rule set, or unknown fields present); on success, clears the covered rule on
   * the builder so CEL won't recompile it.
   */
  static @Nullable Evaluator tryBuild(RuleBase base, FieldRules.Builder rulesBuilder) {
    if (!rulesBuilder.hasBool()) {
      return null;
    }
    BoolRules boolRules = rulesBuilder.getBool();
    if (!boolRules.getUnknownFields().isEmpty()) {
      return null;
    }
    if (!boolRules.hasConst()) {
      return null;
    }
    boolean expected = boolRules.getConst();
    rulesBuilder.setBool(boolRules.toBuilder().clearConst().build());
    return new BoolRulesEvaluator(base, expected);
  }

  @Override
  public boolean tautology() {
    return false;
  }

  @Override
  public List<RuleViolation.Builder> evaluate(Value val, boolean failFast) {
    boolean actual = (Boolean) val.rawValue();
    if (actual == expected) {
      return RuleViolation.NO_VIOLATIONS;
    }
    return base.done(
        Collections.singletonList(
            NativeViolations.newViolation(
                CONST_SITE, null, "must equal " + expected, val, expected)));
  }
}
