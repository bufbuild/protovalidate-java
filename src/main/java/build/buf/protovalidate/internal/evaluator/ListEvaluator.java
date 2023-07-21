// Copyright 2023 Buf Technologies, Inc.
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

import build.buf.gen.buf.validate.FieldConstraints;
import build.buf.gen.buf.validate.Violation;
import build.buf.protovalidate.ValidationResult;
import build.buf.protovalidate.exceptions.ExecutionException;
import com.google.protobuf.Descriptors;
import java.util.ArrayList;
import java.util.List;

/** Performs validation on the elements of a repeated field. */
class ListEvaluator implements Evaluator {

  /** Constraints are checked on every item of the list. */
  final ValueEvaluator itemConstraints;

  /**
   * Constructs a {@link ListEvaluator}.
   *
   * @param fieldConstraints The field constraints to apply to each item in the list.
   * @param fieldDescriptor The descriptor of the repeated field being evaluated.
   */
  ListEvaluator(FieldConstraints fieldConstraints, Descriptors.FieldDescriptor fieldDescriptor) {
    this.itemConstraints = new ValueEvaluator(fieldConstraints, fieldDescriptor);
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
          ErrorPathUtils.prefixErrorPaths(evalResult.getViolations(), "[%d]", i);
      if (failFast && !violations.isEmpty()) {
        return evalResult;
      }
      allViolations.addAll(violations);
    }
    return new ValidationResult(allViolations);
  }
}
