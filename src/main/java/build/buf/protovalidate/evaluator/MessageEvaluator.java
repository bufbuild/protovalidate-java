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

/**
 * Performs validation on a {@link com.google.protobuf.Message}.
 */
class MessageEvaluator implements Evaluator {
    /**
     * List of {@link Evaluator}s that are applied to a message.
     */
    private final List<Evaluator> evaluators = new ArrayList<>();

    @Override
    public boolean tautology() {
        for (Evaluator evaluator : evaluators) {
            if (!evaluator.tautology()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ValidationResult evaluate(Value val, boolean failFast) throws ExecutionException {
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
        evaluators.add(eval);
    }
}
