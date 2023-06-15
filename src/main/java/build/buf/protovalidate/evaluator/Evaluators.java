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

import build.buf.protovalidate.ValidationResult;

import java.util.List;

// TODO: Extra layer
class Evaluators implements Evaluator {
    final List<Evaluator> evaluators;

    public Evaluators(List<Evaluator> evaluators) {
        this.evaluators = evaluators;
    }

    @Override
    public boolean tautology() {
        for (Evaluator eval : evaluators) {
            if (!eval.tautology()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ValidationResult evaluate(JavaValue val, boolean failFast) {
        for (Evaluator evaluator : evaluators) {
            ValidationResult evaluate = evaluator.evaluate(val, failFast);
            // TODO: handle non-fail fast scenarios. failing fast always here.
            if (evaluate.isFailure()) {
                return evaluate;
            }
        }
        return ValidationResult.success();
    }

    @Override
    public void append(Evaluator eval) {
        if (eval != null && !eval.tautology()) {
            this.evaluators.add(eval);
        }
    }
}
