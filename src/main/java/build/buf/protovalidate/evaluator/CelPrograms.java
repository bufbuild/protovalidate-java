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

import build.buf.gen.buf.validate.Violation;
import build.buf.protovalidate.expression.CompiledProgram;
import build.buf.protovalidate.expression.Variable;
import build.buf.protovalidate.results.ExecutionException;
import build.buf.protovalidate.results.ValidationResult;

import java.util.List;

/**
 * Evaluator that executes a {@link build.buf.protovalidate.expression.CompiledProgram}.
 */
class CelPrograms implements Evaluator {
    private final List<CompiledProgram> programs;

    public CelPrograms(List<CompiledProgram> compiledPrograms) {
        this.programs = compiledPrograms;
    }

    @Override
    public boolean tautology() {
        return programs.isEmpty();
    }

    @Override
    public ValidationResult evaluate(Value val, boolean failFast) throws ExecutionException {
        Variable activation = Variable.newThisVariable(val.value());
        ValidationResult evalResult = new ValidationResult();
        for (CompiledProgram program : programs) {
            Violation violation = program.eval(activation);
            if (violation != null) {
                evalResult.addViolation(violation);
                if (failFast) {
                    break;
                }
            }
        }
        return evalResult;
    }

    @Override
    public void append(Evaluator eval) {
        throw new UnsupportedOperationException("append not supported for CelPrograms");
    }
}
