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

import build.buf.protovalidate.results.ExecutionException;
import build.buf.protovalidate.results.ValidationResult;

import java.util.ArrayList;
import java.util.List;

class ValueEvaluator implements Evaluator {
    // Zero is the default or zero-value for this value's type
    Object zero;
    // Constraints are the individual evaluators applied to a value
    private final List<Evaluator> evaluators = new ArrayList<>();
    // TODO: This gets mutated on the fly. Figure out how to manage this better.
    // IgnoreEmpty indicates that the Constraints should not be applied if the
    // field is unset or the default (typically zero) value.
    public boolean ignoreEmpty;

    ValueEvaluator() {
        this.zero = null;
        this.ignoreEmpty = false;
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
