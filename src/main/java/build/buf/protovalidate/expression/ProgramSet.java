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

package build.buf.protovalidate.expression;

import build.buf.protovalidate.results.ExecutionException;
import build.buf.protovalidate.results.ValidationResult;
import build.buf.protovalidate.evaluator.JavaValue;
import build.buf.validate.Violation;
import com.google.protobuf.Message;

import java.util.List;

// ProgramSet is a list of compiledProgram expressions that are evaluated
// together with the same input value. All expressions in a ProgramSet may refer
// to a `this` variable.
public class ProgramSet {
    public final List<CompiledProgram> programs;

    public ProgramSet(List<CompiledProgram> programs) {
        this.programs = programs;
    }

    public ValidationResult evalMessage(Message val, boolean failFast) throws ExecutionException {
        return evaluate(failFast, val);
    }

    public ValidationResult evalValue(JavaValue val, boolean failFast) throws ExecutionException {
        return evaluate(failFast, val.value());
    }

    private ValidationResult evaluate(boolean failFast, Object value) throws ExecutionException {
        Variable activation = new Variable(new NowVariable(), "this", value);
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

    public boolean isEmpty() {
        return programs.isEmpty();
    }
}