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

package build.buf.protovalidate.evaluator;

import build.buf.gen.buf.validate.FieldConstraints;
import build.buf.gen.buf.validate.Violation;
import build.buf.protovalidate.results.ExecutionException;
import build.buf.protovalidate.results.ValidationResult;
import com.google.protobuf.Descriptors;
import java.util.ArrayList;
import java.util.List;

/** Performs validation on the elements of a repeated field. */
class ListEvaluator implements Evaluator {

  /** Constraint are checked on every item of the list. */
  final ValueEvaluator itemConstraints;

  /** Constructs a {@link ListEvaluator}. */
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
      List<Violation> violations =
          ErrorPathUtils.prefixErrorPaths(evalResult.violations, "[%d]", i);
      if (failFast && !violations.isEmpty()) {
        return evalResult;
      }
      allViolations.addAll(violations);
    }
    return new ValidationResult(allViolations);
  }

  @Override
  public void append(Evaluator eval) {
    throw new UnsupportedOperationException("append not supported for ListItems");
  }
}
