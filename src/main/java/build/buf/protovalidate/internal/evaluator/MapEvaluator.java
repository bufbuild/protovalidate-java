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
import build.buf.validate.FieldConstraints;
import build.buf.validate.Violation;
import com.google.protobuf.Descriptors;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Performs validation on a map field's key-value pairs. */
class MapEvaluator implements Evaluator {
  /** Constraint for checking the map keys */
  private final ValueEvaluator keyEvaluator;

  /** Constraint for checking the map values */
  private final ValueEvaluator valueEvaluator;

  /**
   * Constructs a {@link MapEvaluator}.
   *
   * @param fieldConstraints The field constraints to apply to the map.
   * @param fieldDescriptor The descriptor of the map field being evaluated.
   */
  MapEvaluator(FieldConstraints fieldConstraints, Descriptors.FieldDescriptor fieldDescriptor) {
    this.keyEvaluator = new ValueEvaluator();
    this.valueEvaluator = new ValueEvaluator();
  }

  /**
   * Gets the key evaluator associated with this map evaluator.
   *
   * @return The key evaluator.
   */
  public ValueEvaluator getKeyEvaluator() {
    return keyEvaluator;
  }

  /**
   * Gets the value evaluator associated with this map evaluator.
   *
   * @return The value evaluator.
   */
  public ValueEvaluator getValueEvaluator() {
    return valueEvaluator;
  }

  @Override
  public boolean tautology() {
    return keyEvaluator.tautology() && valueEvaluator.tautology();
  }

  @Override
  public ValidationResult evaluate(Value val, boolean failFast) throws ExecutionException {
    List<Violation> violations = new ArrayList<>();
    Map<Value, Value> mapValue = val.mapValue();
    for (Map.Entry<Value, Value> entry : mapValue.entrySet()) {
      violations.addAll(evalPairs(entry.getKey(), entry.getValue(), failFast));
      if (failFast && !violations.isEmpty()) {
        return new ValidationResult(violations);
      }
    }
    if (violations.isEmpty()) {
      return ValidationResult.EMPTY;
    }
    return new ValidationResult(violations);
  }

  private List<Violation> evalPairs(Value key, Value value, boolean failFast)
      throws ExecutionException {
    List<Violation> keyViolations = keyEvaluator.evaluate(key, failFast).getViolations();
    final List<Violation> valueViolations;
    if (failFast && !keyViolations.isEmpty()) {
      // Don't evaluate value constraints if failFast is enabled and keys failed validation.
      // We still need to continue execution to the end to properly prefix violation field paths.
      valueViolations = Collections.emptyList();
    } else {
      valueViolations = valueEvaluator.evaluate(value, failFast).getViolations();
    }
    if (keyViolations.isEmpty() && valueViolations.isEmpty()) {
      return Collections.emptyList();
    }
    List<Violation> violations = new ArrayList<>(keyViolations.size() + valueViolations.size());
    violations.addAll(keyViolations);
    violations.addAll(valueViolations);

    Object keyName = key.value(Object.class);
    if (keyName == null) {
      return Collections.emptyList();
    }
    List<Violation> prefixedViolations;
    if (keyName instanceof Number) {
      prefixedViolations = ErrorPathUtils.prefixErrorPaths(violations, "[%s]", keyName);
    } else {
      prefixedViolations = ErrorPathUtils.prefixErrorPaths(violations, "[\"%s\"]", keyName);
    }
    return prefixedViolations;
  }
}
