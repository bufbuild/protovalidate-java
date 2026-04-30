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

import build.buf.protovalidate.RuleViolation;
import build.buf.protovalidate.Value;
import org.jspecify.annotations.Nullable;

/**
 * Builds {@link RuleViolation.Builder} instances for native rule evaluators.
 *
 * <p>The resulting builder carries only rule-relative state: rule id, message, the rule path suffix
 * from {@link RuleSite}, and optional field/rule values. Field path and any nested-rule prefix are
 * prepended later by {@code FieldPathUtils.updatePaths} when the violations leave the native
 * evaluator's {@code evaluate} — the same pattern {@code CelPrograms} uses.
 */
final class NativeViolations {
  private NativeViolations() {}

  /**
   * Builds a violation for a rule failure. If {@link RuleSite#getRuleId()} or {@link
   * RuleSite#getMessage()} return non-null they take precedence over the supplied {@code ruleId}
   * and {@code message} arguments; this lets a {@link RuleSite} pre-bake constant text and skip the
   * per-call argument when there's nothing dynamic to report.
   *
   * @param site the rule site (rule path suffix + leaf descriptor + optional pre-baked id/message)
   * @param ruleId rule id to use when the site doesn't have one pre-baked
   * @param message violation message to use when the site doesn't have one pre-baked
   * @param fieldValue the failing field value (its descriptor is used to populate {@code
   *     field_value} on the violation); pass null to omit
   * @param ruleValue the rule's bound value (e.g. the {@code 5} in {@code min_len = 5}), bound to
   *     the site's leaf descriptor; pass null to omit
   */
  static RuleViolation.Builder newViolation(
      RuleSite site,
      @Nullable String ruleId,
      @Nullable String message,
      @Nullable Value fieldValue,
      @Nullable Object ruleValue) {
    String effectiveRuleId = (site.getRuleId() != null) ? site.getRuleId() : ruleId;
    String effectiveMessage = (site.getMessage() != null) ? site.getMessage() : message;

    RuleViolation.Builder builder = RuleViolation.newBuilder();
    if (effectiveRuleId != null) {
      builder.setRuleId(effectiveRuleId);
    }
    if (effectiveMessage != null) {
      builder.setMessage(effectiveMessage);
    }
    builder.addAllRulePathElements(site.getPathElements());
    if (fieldValue != null && fieldValue.fieldDescriptor() != null) {
      builder.setFieldValue(new RuleViolation.FieldValue(fieldValue));
    }
    if (ruleValue != null) {
      builder.setRuleValue(new RuleViolation.FieldValue(ruleValue, site.getLeafDescriptor()));
    }
    return builder;
  }
}
