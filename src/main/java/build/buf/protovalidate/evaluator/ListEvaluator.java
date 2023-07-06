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

import java.util.List;

public class ListEvaluator implements Evaluator {

    // ItemConstraints are checked on every item of the list
    public final ValueEvaluator itemConstraints;

    public ListEvaluator() {
        this.itemConstraints = new ValueEvaluator();
    }

    @Override
    public boolean tautology() {
        return itemConstraints.tautology();
    }

    @Override
    public ValidationResult evaluate(JavaValue val, boolean failFast) throws ExecutionException {
        ValidationResult validationResult = new ValidationResult();

        List<JavaValue> repeatedValues = val.repeatedValue();
        for (int i = 0; i < repeatedValues.size(); i++) {
            ValidationResult evalResult = itemConstraints.evaluate(repeatedValues.get(i), failFast);
            evalResult.prefixErrorPaths("[%d]", i);
            if (!validationResult.merge(evalResult, failFast)) {
                return evalResult;
            }
        }
        return validationResult;
    }

    @Override
    public void append(Evaluator eval) {
        throw new UnsupportedOperationException("append not supported for ListItems");
    }
}
