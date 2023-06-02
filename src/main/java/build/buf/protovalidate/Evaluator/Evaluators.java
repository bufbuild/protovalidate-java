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

package build.buf.protovalidate.Evaluator;

import build.buf.protovalidate.Errors.ValidationError;
import build.buf.protovalidate.Evaluator.Evaluator;
import com.google.protobuf.DynamicMessage;

import java.util.List;

class Evaluators implements Evaluator {
    private List<Evaluator> evaluators;

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
    public void evaluate(DynamicMessage val, boolean failFast) throws ValidationError {
    }

    public void append(MessageEvaluator eval) {
        if (eval != null && !eval.tautology()) {
            this.evaluators.add(eval);
        }
    }

}
