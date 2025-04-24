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

package build.buf.protovalidate;

import build.buf.protovalidate.exceptions.ExecutionException;
import build.buf.validate.FieldPath;
import com.google.protobuf.Descriptors;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * {@link ValueEvaluator} performs validation on any concrete value contained within a singular
 * field, repeated elements, or the keys/values of a map.
 */
class ValueEvaluator implements Evaluator {
  /** The {@link Descriptors.FieldDescriptor} targeted by this evaluator */
  private final Descriptors.@Nullable FieldDescriptor descriptor;

  /** The nested rule path that this value evaluator is for */
  @Nullable private final FieldPath nestedRule;

  /** The default or zero-value for this value's type. */
  @Nullable private Object zero;

  /** The evaluators applied to a value. */
  private final List<Evaluator> evaluators = new ArrayList<>();

  /**
   * Indicates that the Rules should not be applied if the field is unset or the default (typically
   * zero) value.
   */
  private boolean ignoreEmpty;

  /** Constructs a {@link ValueEvaluator}. */
  ValueEvaluator(Descriptors.@Nullable FieldDescriptor descriptor, @Nullable FieldPath nestedRule) {
    this.descriptor = descriptor;
    this.nestedRule = nestedRule;
  }

  public Descriptors.@Nullable FieldDescriptor getDescriptor() {
    return descriptor;
  }

  public @Nullable FieldPath getNestedRule() {
    return nestedRule;
  }

  public boolean hasNestedRule() {
    return this.nestedRule != null;
  }

  @Override
  public boolean tautology() {
    return evaluators.isEmpty();
  }

  @Override
  public List<RuleViolation.Builder> evaluate(Value val, boolean failFast)
      throws ExecutionException {
    if (this.shouldIgnore(val.value(Object.class))) {
      return RuleViolation.NO_VIOLATIONS;
    }
    List<RuleViolation.Builder> allViolations = new ArrayList<>();
    for (Evaluator evaluator : evaluators) {
      List<RuleViolation.Builder> violations = evaluator.evaluate(val, failFast);
      if (failFast && !violations.isEmpty()) {
        return violations;
      }
      allViolations.addAll(violations);
    }
    if (allViolations.isEmpty()) {
      return RuleViolation.NO_VIOLATIONS;
    }
    return allViolations;
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

  public void setIgnoreEmpty(Object zero) {
    this.ignoreEmpty = true;
    this.zero = zero;
  }

  private boolean shouldIgnore(Object value) {
    return this.ignoreEmpty && Objects.equals(value, this.zero);
  }
}
