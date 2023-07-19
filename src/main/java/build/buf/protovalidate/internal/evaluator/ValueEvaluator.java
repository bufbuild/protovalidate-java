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
import com.google.protobuf.DynamicMessage;
import java.util.ArrayList;
import java.util.List;
import org.projectnessie.cel.common.ULong;

/**
 * {@link ValueEvaluator} performs validation on any concrete value contained within a singular
 * field, repeated elements, or the keys/values of a map.
 */
class ValueEvaluator implements Evaluator {
  private static final ULong ULONG_ZERO = ULong.valueOf(0L);

  /** The default or zero-value for this value's type. */
  private final Object zero;

  /** The evaluators applied to a value. */
  private final List<Evaluator> evaluators = new ArrayList<>();

  /**
   * Indicates that the Constraints should not be applied if the field is unset or the default
   * (typically zero) value.
   */
  private final boolean ignoreEmpty;

  /** Constructs a {@link ValueEvaluator}. */
  ValueEvaluator(FieldConstraints fieldConstraints, Descriptors.FieldDescriptor fieldDescriptor) {
    Descriptors.FieldDescriptor.Type type = fieldDescriptor.getType();
    if (type == Descriptors.FieldDescriptor.Type.MESSAGE) {
      DynamicMessage message =
          DynamicMessage.getDefaultInstance(fieldDescriptor.getContainingType());
      this.zero = message.getField(fieldDescriptor);
    } else {
      if (!fieldDescriptor.isRepeated()
          && (type == Descriptors.FieldDescriptor.Type.UINT32
              || type == Descriptors.FieldDescriptor.Type.UINT64
              || type == Descriptors.FieldDescriptor.Type.FIXED32
              || type == Descriptors.FieldDescriptor.Type.FIXED64)) {
        this.zero = ULONG_ZERO;
      } else {
        this.zero = fieldDescriptor.getDefaultValue();
      }
    }
    this.ignoreEmpty = fieldConstraints.getIgnoreEmpty();
  }

  public boolean getIgnoreEmpty() {
    return ignoreEmpty;
  }

  @Override
  public boolean tautology() {
    return evaluators.isEmpty();
  }

  @Override
  public ValidationResult evaluate(Value val, boolean failFast) throws ExecutionException {
    if (ignoreEmpty && isZero(val)) {
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

  public void append(Evaluator eval) {
    if (!eval.tautology()) {
      this.evaluators.add(eval);
    }
  }

  private boolean isZero(Value val) {
    if (val == null) {
      return false;
    }
    if (zero == null) {
      return val.value(Object.class) == null;
    }
    return zero.equals(val.value(zero.getClass()));
  }
}
