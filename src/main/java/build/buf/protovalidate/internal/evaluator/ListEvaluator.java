// Copyright 2023-2024 Buf Technologies, Inc.
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

package build.buf.protovalidate.internal.evaluator;

import build.buf.protovalidate.ValidationResult;
import build.buf.protovalidate.Violation;
import build.buf.protovalidate.exceptions.ExecutionException;
import build.buf.protovalidate.internal.errors.FieldPathUtils;
import build.buf.validate.FieldConstraints;
import build.buf.validate.FieldPath;
import build.buf.validate.FieldPathElement;
import build.buf.validate.RepeatedRules;
import com.google.protobuf.Descriptors;
import java.util.ArrayList;
import java.util.List;

/** Performs validation on the elements of a repeated field. */
class ListEvaluator implements Evaluator {
  /** Rule path to repeated items rules */
  private static final List<FieldPathElement> REPEATED_ITEMS_RULE_PATH =
      FieldPath.newBuilder()
          .addElements(
              FieldPathUtils.fieldPathElement(
                  FieldConstraints.getDescriptor()
                      .findFieldByNumber(FieldConstraints.REPEATED_FIELD_NUMBER)))
          .addElements(
              FieldPathUtils.fieldPathElement(
                  RepeatedRules.getDescriptor()
                      .findFieldByNumber(RepeatedRules.ITEMS_FIELD_NUMBER)))
          .build()
          .getElementsList();

  public static final EvaluatorWrapper ITEMS_WRAPPER = new ItemsWrapper();

  private static class ItemsEvaluator implements Evaluator {
    /** Evaluator to wrap */
    private final Evaluator evaluator;

    public ItemsEvaluator(Evaluator evaluator) {
      this.evaluator = evaluator;
    }

    @Override
    public boolean tautology() {
      return this.evaluator.tautology();
    }

    @Override
    public ValidationResult evaluate(Value val, boolean failFast) throws ExecutionException {
      ValidationResult result = evaluator.evaluate(val, failFast);
      if (result.isSuccess()) {
        return result;
      }
      return new ValidationResult(
          FieldPathUtils.prependRulePaths(result.getViolations(), REPEATED_ITEMS_RULE_PATH));
    }
  }

  private static class ItemsWrapper implements EvaluatorWrapper {
    @Override
    public Evaluator wrap(Evaluator evaluator) {
      return new ItemsEvaluator(evaluator);
    }
  }

  /** Constraints are checked on every item of the list. */
  final ValueEvaluator itemConstraints;

  /** Field descriptor of the list field. */
  final Descriptors.FieldDescriptor fieldDescriptor;

  /** Constructs a {@link ListEvaluator}. */
  ListEvaluator(Descriptors.FieldDescriptor fieldDescriptor) {
    this.itemConstraints = new ValueEvaluator();
    this.fieldDescriptor = fieldDescriptor;
  }

  @Override
  public boolean tautology() {
    return itemConstraints.tautology();
  }

  @Override
  public ValidationResult evaluate(Value val, boolean failFast) throws ExecutionException {
    List<Violation> allViolations = new ArrayList<>();
    List<Value> repeatedValues = val.repeatedValue();
    for (int i = 0; i < repeatedValues.size(); i++) {
      ValidationResult evalResult = itemConstraints.evaluate(repeatedValues.get(i), failFast);
      if (evalResult.getViolations().isEmpty()) {
        continue;
      }
      List<Violation> violations =
          FieldPathUtils.prependFieldPaths(
              evalResult.getViolations(),
              FieldPathUtils.fieldPathElement(fieldDescriptor).setIndex(i).build(),
              false);
      if (failFast && !violations.isEmpty()) {
        return evalResult;
      }
      allViolations.addAll(violations);
    }
    return new ValidationResult(allViolations);
  }
}
