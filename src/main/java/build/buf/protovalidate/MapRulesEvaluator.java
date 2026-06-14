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
import build.buf.validate.MapRules;
import com.google.protobuf.Descriptors.FieldDescriptor;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Native evaluator for map-level rules: {@code min_pairs} and {@code max_pairs}. Key/value rules
 * continue to flow through {@link build.buf.protovalidate.MapEvaluator} and the inner key/value
 * {@link build.buf.protovalidate.ValueEvaluator}s. Mirrors {@code nativeMapEval} in
 * protovalidate-go's {@code native_map.go}.
 */
final class MapRulesEvaluator implements Evaluator {
  private static final FieldDescriptor MAP_RULES_DESC =
      FieldRules.getDescriptor().findFieldByNumber(FieldRules.MAP_FIELD_NUMBER);

  private static final RuleSite MIN_PAIRS_SITE =
      RuleSite.of(
          MAP_RULES_DESC,
          MapRules.getDescriptor().findFieldByNumber(MapRules.MIN_PAIRS_FIELD_NUMBER),
          "map.min_pairs",
          null);
  private static final RuleSite MAX_PAIRS_SITE =
      RuleSite.of(
          MAP_RULES_DESC,
          MapRules.getDescriptor().findFieldByNumber(MapRules.MAX_PAIRS_FIELD_NUMBER),
          "map.max_pairs",
          null);

  private final RuleBase base;
  private final @Nullable Long minPairs;
  private final @Nullable Long maxPairs;

  private MapRulesEvaluator(RuleBase base, @Nullable Long minPairs, @Nullable Long maxPairs) {
    this.base = base;
    this.minPairs = minPairs;
    this.maxPairs = maxPairs;
  }

  static @Nullable Evaluator tryBuild(RuleBase base, FieldRules.Builder rulesBuilder) {
    if (!rulesBuilder.hasMap()) {
      return null;
    }
    MapRules rules = rulesBuilder.getMap();
    if (!rules.getUnknownFields().isEmpty()) {
      return null;
    }

    MapRules.Builder mb = rules.toBuilder();
    boolean hasRule = false;

    Long minPairs = null;
    if (rules.hasMinPairs()) {
      minPairs = rules.getMinPairs();
      mb.clearMinPairs();
      hasRule = true;
    }

    Long maxPairs = null;
    if (rules.hasMaxPairs()) {
      maxPairs = rules.getMaxPairs();
      mb.clearMaxPairs();
      hasRule = true;
    }

    if (!hasRule) {
      return null;
    }
    rulesBuilder.setMap(mb.build());
    return new MapRulesEvaluator(base, minPairs, maxPairs);
  }

  @Override
  public boolean tautology() {
    // tryBuild returns null when neither field is set, so this evaluator is never built
    // without at least one rule active. Match the rest of the rules package by returning false
    // unconditionally.
    return false;
  }

  @Override
  public List<RuleViolation.Builder> evaluate(Value val, boolean failFast) {
    // Java protobuf returns map fields as a List of synthetic key/value entry messages; the size
    // is the pair count.
    List<?> entries = (List<?>) val.rawValue();
    long size = entries.size();
    List<RuleViolation.Builder> violations = null;

    if (minPairs != null && size < minPairs) {
      violations =
          RuleBase.add(
              violations,
              NativeViolations.newViolation(
                  MIN_PAIRS_SITE,
                  null,
                  "map must be at least " + minPairs + " entries",
                  val,
                  minPairs));
      if (failFast) return base.done(violations);
    }

    if (maxPairs != null && size > maxPairs) {
      violations =
          RuleBase.add(
              violations,
              NativeViolations.newViolation(
                  MAX_PAIRS_SITE,
                  null,
                  "map must be at most " + maxPairs + " entries",
                  val,
                  maxPairs));
      if (failFast) return base.done(violations);
    }

    return base.done(violations);
  }
}
