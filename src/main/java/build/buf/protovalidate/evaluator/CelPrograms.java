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
import build.buf.protovalidate.expression.CompiledProgramSet;
import com.google.protobuf.Message;

/**
 * Evaluator that executes a {@link build.buf.protovalidate.expression.CompiledProgramSet}.
 */
class CelPrograms implements Evaluator, MessageEvaluator {
    private final CompiledProgramSet compiledProgramSet;

    /**
     * Constructs a new evaluator for a {@link CompiledProgramSet}.
     */
    CelPrograms(CompiledProgramSet compiledProgramSet) {
        // TODO: require non null somehow? is this todo complete?
        if (compiledProgramSet == null) {
            throw new IllegalArgumentException("programSet cannot be null");
        }
        this.compiledProgramSet = compiledProgramSet;
    }

    public boolean tautology() {
        return compiledProgramSet.isEmpty();
    }

    @Override
    public ValidationResult evaluate(Value val, boolean failFast) throws ExecutionException {
        return compiledProgramSet.evalValue(val.value(), failFast);
    }

    @Override
    public void append(Evaluator eval) {
        throw new UnsupportedOperationException("append not supported for CelPrograms");
    }

    @Override
    public ValidationResult evaluateMessage(Message val, boolean failFast) throws ExecutionException {
        return compiledProgramSet.evalMessage(val, failFast);
    }
}
