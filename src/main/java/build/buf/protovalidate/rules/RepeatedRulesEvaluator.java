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
import build.buf.validate.RepeatedRules;
import com.google.protobuf.Descriptors.FieldDescriptor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Native evaluator for repeated list-level rules: {@code min_items}, {@code max_items}, {@code
 * unique}. Element-level rules continue to flow through {@code ListEvaluator} and the inner {@link
 * build.buf.protovalidate.ValueEvaluator}.
 *
 * <p>The {@code unique} rule is only supported when the element kind has well-defined value
 * equality (scalars, strings, bytes, bools, enums). Message/group element kinds fall back to CEL.
 * Mirrors {@code nativeRepeatedEval} in protovalidate-go's {@code native_repeated.go}.
 */
final class RepeatedRulesEvaluator implements Evaluator {
  private static final FieldDescriptor REPEATED_RULES_DESC =
      FieldRules.getDescriptor().findFieldByNumber(FieldRules.REPEATED_FIELD_NUMBER);

  private static final RuleSite MIN_ITEMS_SITE =
      RuleSite.of(
          REPEATED_RULES_DESC,
          RepeatedRules.getDescriptor().findFieldByNumber(RepeatedRules.MIN_ITEMS_FIELD_NUMBER),
          "repeated.min_items",
          null);
  private static final RuleSite MAX_ITEMS_SITE =
      RuleSite.of(
          REPEATED_RULES_DESC,
          RepeatedRules.getDescriptor().findFieldByNumber(RepeatedRules.MAX_ITEMS_FIELD_NUMBER),
          "repeated.max_items",
          null);
  private static final RuleSite UNIQUE_SITE =
      RuleSite.of(
          REPEATED_RULES_DESC,
          RepeatedRules.getDescriptor().findFieldByNumber(RepeatedRules.UNIQUE_FIELD_NUMBER),
          "repeated.unique",
          "repeated value must contain unique items");

  /** Below this list size, the linear-scan unique check beats {@link HashSet} (no allocation). */
  private static final int UNIQUE_LINEAR_THRESHOLD = 16;

  private final RuleBase base;
  private final @Nullable Long minItems;
  private final @Nullable Long maxItems;
  private final boolean unique;

  private RepeatedRulesEvaluator(
      RuleBase base, @Nullable Long minItems, @Nullable Long maxItems, boolean unique) {
    this.base = base;
    this.minItems = minItems;
    this.maxItems = maxItems;
    this.unique = unique;
  }

  static @Nullable Evaluator tryBuild(RuleBase base, FieldRules.Builder rulesBuilder) {
    if (!rulesBuilder.hasRepeated()) {
      return null;
    }
    RepeatedRules rules = rulesBuilder.getRepeated();
    if (!rules.getUnknownFields().isEmpty()) {
      return null;
    }

    RepeatedRules.Builder rb = rules.toBuilder();
    boolean hasRule = false;

    Long minItems = null;
    if (rules.hasMinItems()) {
      minItems = rules.getMinItems();
      rb.clearMinItems();
      hasRule = true;
    }

    Long maxItems = null;
    if (rules.hasMaxItems()) {
      maxItems = rules.getMaxItems();
      rb.clearMaxItems();
      hasRule = true;
    }

    boolean unique = false;
    if (rules.getUnique()) {
      // Element kind must support reliable Object.equals — scalars, strings, bools, bytes, enums
      // all do (ByteString and EnumValueDescriptor have correct equals/hashCode). Messages don't,
      // so fall through to CEL.
      FieldDescriptor descriptor = base.getDescriptor();
      if (descriptor == null || !isUniqueSupported(descriptor.getType())) {
        return null;
      }
      unique = true;
      rb.clearUnique();
      hasRule = true;
    }

    if (!hasRule) {
      return null;
    }
    rulesBuilder.setRepeated(rb.build());
    return new RepeatedRulesEvaluator(base, minItems, maxItems, unique);
  }

  private static boolean isUniqueSupported(FieldDescriptor.Type type) {
    switch (type) {
      case INT32:
      case SINT32:
      case SFIXED32:
      case INT64:
      case SINT64:
      case SFIXED64:
      case UINT32:
      case FIXED32:
      case UINT64:
      case FIXED64:
      case FLOAT:
      case DOUBLE:
      case BOOL:
      case STRING:
      case BYTES:
      case ENUM:
        return true;
      case MESSAGE:
      case GROUP:
      default:
        return false;
    }
  }

  @Override
  public boolean tautology() {
    return false;
  }

  @Override
  public List<RuleViolation.Builder> evaluate(Value val, boolean failFast) {
    List<?> list = (List<?>) val.rawValue();
    long size = list.size();
    List<RuleViolation.Builder> violations = null;

    if (minItems != null && size < minItems) {
      violations =
          add(
              violations,
              NativeViolations.newViolation(
                  MIN_ITEMS_SITE,
                  null,
                  "must contain at least " + minItems + " item(s)",
                  val,
                  minItems));
      if (failFast) return done(violations);
    }

    if (maxItems != null && size > maxItems) {
      violations =
          add(
              violations,
              NativeViolations.newViolation(
                  MAX_ITEMS_SITE,
                  null,
                  "must contain no more than " + maxItems + " item(s)",
                  val,
                  maxItems));
      if (failFast) return done(violations);
    }

    if (unique && !isUnique(list)) {
      violations =
          add(violations, NativeViolations.newViolation(UNIQUE_SITE, null, null, val, true));
      if (failFast) return done(violations);
    }

    return done(violations);
  }

  /**
   * Returns true iff every element in {@code list} is distinct. Below {@link
   * #UNIQUE_LINEAR_THRESHOLD} elements uses an O(n²) scan with no auxiliary allocation; above that
   * uses a {@link HashSet}.
   */
  private static boolean isUnique(List<?> list) {
    int size = list.size();
    if (size <= 1) {
      return true;
    }
    if (size <= UNIQUE_LINEAR_THRESHOLD) {
      for (int i = 1; i < size; i++) {
        Object current = list.get(i);
        for (int j = 0; j < i; j++) {
          if (current.equals(list.get(j))) {
            return false;
          }
        }
      }
      return true;
    }
    Set<Object> seen = new HashSet<>(size);
    for (Object element : list) {
      if (!seen.add(element)) {
        return false;
      }
    }
    return true;
  }

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
