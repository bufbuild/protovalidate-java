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

import build.buf.protovalidate.ValidationResult;
import build.buf.protovalidate.exceptions.ExecutionException;
import build.buf.validate.Violation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * {@link ValueEvaluator} performs validation on any concrete value contained within a singular
 * field, repeated elements, or the keys/values of a map.
 */
class ValueEvaluator implements Evaluator {
  /** The default or zero-value for this value's type. */
  @Nullable private Object zero;

  /** The evaluators applied to a value. */
  private final List<Evaluator> evaluators = new ArrayList<>();

  /**
   * Indicates that the Constraints should not be applied if the field is unset or the default
   * (typically zero) value.
   */
  private boolean ignoreEmpty;

  /** Constructs a {@link ValueEvaluator}. */
  ValueEvaluator() {}

  @Override
  public boolean tautology() {
    return evaluators.isEmpty();
  }

  @Override
  public ValidationResult evaluate(Value val, boolean failFast) throws ExecutionException {
    if (this.shouldIgnore(val.value(Object.class))) {
      return ValidationResult.EMPTY;
    }
    List<Violation> violations = new ArrayList<>();
    for (Evaluator evaluator : evaluators) {
      ValidationResult evalResult = evaluator.evaluate(val, failFast);
      if (failFast && !evalResult.getViolations().isEmpty()) {
        return evalResult;
      }
      violations.addAll(evalResult.getViolations());
    }
    if (violations.isEmpty()) {
      return ValidationResult.EMPTY;
    }
    return new ValidationResult(violations);
  }

  /**
   * Appends an evaluator to the list of evaluators.
   *
   * @param eval The evaluator to append.
   */
  public void append(Evaluator eval) {
    if (!eval.tautology()) {
      this.evaluators.add(eval);
    }
  }

  public void setIgnoreEmpty(@Nullable Object zero) {
    this.ignoreEmpty = true;
    this.zero = zero;
  }

  private boolean shouldIgnore(Object value) {
    return this.ignoreEmpty && Objects.equals(value, this.zero);
  }
}
