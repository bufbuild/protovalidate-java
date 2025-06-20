// Copyright 2023-2025 Buf Technologies, Inc.
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

import build.buf.protovalidate.exceptions.ExecutionException;
import build.buf.validate.FieldPath;
import build.buf.validate.FieldPathElement;
import build.buf.validate.FieldRules;
import build.buf.validate.RepeatedRules;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Performs validation on the elements of a repeated field. */
final class ListEvaluator implements Evaluator {
  /** Rule path to repeated rules */
  private static final FieldPath REPEATED_ITEMS_RULE_PATH =
      FieldPath.newBuilder()
          .addElements(
              FieldPathUtils.fieldPathElement(
                  FieldRules.getDescriptor().findFieldByNumber(FieldRules.REPEATED_FIELD_NUMBER)))
          .addElements(
              FieldPathUtils.fieldPathElement(
                  RepeatedRules.getDescriptor()
                      .findFieldByNumber(RepeatedRules.ITEMS_FIELD_NUMBER)))
          .build();

  private final RuleViolationHelper helper;

  /** Rules are checked on every item of the list. */
  final ValueEvaluator itemRules;

  /** Constructs a {@link ListEvaluator}. */
  ListEvaluator(ValueEvaluator valueEvaluator) {
    this.helper = new RuleViolationHelper(valueEvaluator);
    this.itemRules = new ValueEvaluator(null, REPEATED_ITEMS_RULE_PATH);
  }

  @Override
  public boolean tautology() {
    return itemRules.tautology();
  }

  @Override
  public List<RuleViolation.Builder> evaluate(Value val, boolean failFast)
      throws ExecutionException {
    List<RuleViolation.Builder> allViolations = new ArrayList<>();
    List<Value> repeatedValues = val.repeatedValue();
    for (int i = 0; i < repeatedValues.size(); i++) {
      List<RuleViolation.Builder> violations = itemRules.evaluate(repeatedValues.get(i), failFast);
      if (violations.isEmpty()) {
        continue;
      }
      FieldPathElement fieldPathElement =
          Objects.requireNonNull(helper.getFieldPathElement()).toBuilder().setIndex(i).build();
      FieldPathUtils.updatePaths(violations, fieldPathElement, helper.getRulePrefixElements());
      if (failFast && !violations.isEmpty()) {
        return violations;
      }
      allViolations.addAll(violations);
    }
    return allViolations;
  }
}
