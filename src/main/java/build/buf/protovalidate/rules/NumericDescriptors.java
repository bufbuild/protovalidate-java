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

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import org.jspecify.annotations.Nullable;

/**
 * Pre-built {@link RuleSite}s for a single numeric rule type ({@code Int32Rules}, {@code
 * UInt64Rules}, {@code FloatRules}, etc.). Built once per type at class-init time so violation
 * construction at validation time avoids re-building path-element protos.
 *
 * <p>{@code finiteSite} is null for non-float kinds.
 */
final class NumericDescriptors {
  final RuleSite gtSite;
  final RuleSite gteSite;
  final RuleSite ltSite;
  final RuleSite lteSite;
  final RuleSite constSite;
  final RuleSite inSite;
  final RuleSite notInSite;
  final @Nullable RuleSite finiteSite;
  // Leaf descriptors used to look up rule fields on the *Rules message at build time.
  final FieldDescriptor gtField;
  final FieldDescriptor gteField;
  final FieldDescriptor ltField;
  final FieldDescriptor lteField;
  final FieldDescriptor constField;
  final FieldDescriptor inField;
  final FieldDescriptor notInField;
  final @Nullable FieldDescriptor finiteField;

  private NumericDescriptors(
      RuleSite gtSite,
      RuleSite gteSite,
      RuleSite ltSite,
      RuleSite lteSite,
      RuleSite constSite,
      RuleSite inSite,
      RuleSite notInSite,
      @Nullable RuleSite finiteSite,
      FieldDescriptor gtField,
      FieldDescriptor gteField,
      FieldDescriptor ltField,
      FieldDescriptor lteField,
      FieldDescriptor constField,
      FieldDescriptor inField,
      FieldDescriptor notInField,
      @Nullable FieldDescriptor finiteField) {
    this.gtSite = gtSite;
    this.gteSite = gteSite;
    this.ltSite = ltSite;
    this.lteSite = lteSite;
    this.constSite = constSite;
    this.inSite = inSite;
    this.notInSite = notInSite;
    this.finiteSite = finiteSite;
    this.gtField = gtField;
    this.gteField = gteField;
    this.ltField = ltField;
    this.lteField = lteField;
    this.constField = constField;
    this.inField = inField;
    this.notInField = notInField;
    this.finiteField = finiteField;
  }

  /**
   * Builds the descriptor bundle for a numeric rule type.
   *
   * @param fieldRulesField the {@link FieldDescriptor} of the {@code FieldRules} oneof case (e.g.
   *     the {@code int32} field on {@code FieldRules})
   * @param rulesDescriptor the {@link Descriptor} of the rules message (e.g. {@code Int32Rules})
   * @param typeName the proto rule prefix used in rule ids (e.g. {@code "int32"})
   * @param hasFinite whether this kind supports the {@code finite} rule (only float/double do)
   */
  static NumericDescriptors build(
      FieldDescriptor fieldRulesField,
      Descriptor rulesDescriptor,
      String typeName,
      boolean hasFinite) {
    FieldDescriptor gt = rulesDescriptor.findFieldByName("gt");
    FieldDescriptor gte = rulesDescriptor.findFieldByName("gte");
    FieldDescriptor lt = rulesDescriptor.findFieldByName("lt");
    FieldDescriptor lte = rulesDescriptor.findFieldByName("lte");
    FieldDescriptor constant = rulesDescriptor.findFieldByName("const");
    FieldDescriptor inField = rulesDescriptor.findFieldByName("in");
    FieldDescriptor notInField = rulesDescriptor.findFieldByName("not_in");
    FieldDescriptor finiteField = hasFinite ? rulesDescriptor.findFieldByName("finite") : null;
    return new NumericDescriptors(
        // Sites carry rule-id and rule-path for violation building. Where the rule id is
        // computed dynamically (gt/gte/lt/lte combine into different ids depending on which
        // bounds are active), pass null and let the caller supply per-violation.
        RuleSite.of(fieldRulesField, gt, null, null),
        RuleSite.of(fieldRulesField, gte, null, null),
        RuleSite.of(fieldRulesField, lt, null, null),
        RuleSite.of(fieldRulesField, lte, null, null),
        RuleSite.of(fieldRulesField, constant, typeName + ".const", null),
        RuleSite.of(fieldRulesField, inField, typeName + ".in", null),
        RuleSite.of(fieldRulesField, notInField, typeName + ".not_in", null),
        finiteField != null
            ? RuleSite.of(fieldRulesField, finiteField, typeName + ".finite", "must be finite")
            : null,
        gt,
        gte,
        lt,
        lte,
        constant,
        inField,
        notInField,
        finiteField);
  }
}
