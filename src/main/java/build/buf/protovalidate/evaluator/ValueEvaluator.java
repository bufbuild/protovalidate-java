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
import build.buf.protovalidate.results.ExecutionException;
import build.buf.protovalidate.results.ValidationResult;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * ValueEvaluator performs validation on any concrete value contained within a singular
 * field, repeated elements, or the keys/values of a map.
 */
class ValueEvaluator implements Evaluator {
    /**
     * Zero is the default or zero-value for this value's type
     */
    private final Object zero;
    /**
     * Constraints are the individual evaluators applied to a value
     */
    private final List<Evaluator> evaluators = new ArrayList<>();
    /**
     * IgnoreEmpty indicates that the Constraints should not be applied if the
     * field is unset or the default (typically zero) value.
     */
    private final boolean ignoreEmpty;

    /**
     * ValueEvaluator is a constructor for ValueEvaluator.
     */
    ValueEvaluator(FieldConstraints fieldConstraints, Descriptors.FieldDescriptor fieldDescriptor) {
        if (fieldDescriptor.getType() == Descriptors.FieldDescriptor.Type.MESSAGE) {
            DynamicMessage message = DynamicMessage.getDefaultInstance(fieldDescriptor.getContainingType());
            this.zero = message.getField(fieldDescriptor);
        } else {
            this.zero = fieldDescriptor.getDefaultValue();
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
            return new ValidationResult();
        }
        ValidationResult validationResult = new ValidationResult();
        for (Evaluator evaluator : evaluators) {
            ValidationResult evalResult = evaluator.evaluate(val, failFast);
            if (!validationResult.merge(evalResult, failFast)) {
                return evalResult;
            }
        }
        return validationResult;
    }

    @Override
    public void append(Evaluator eval) {
        if (eval != null && !eval.tautology()) {
            this.evaluators.add(eval);
        }
    }

    private boolean isZero(Value val) {
        if (val == null) {
            return false;
        } else if (zero == null) {
            return val.value() == null;
        }
        return zero.equals(val.value());
    }
}
