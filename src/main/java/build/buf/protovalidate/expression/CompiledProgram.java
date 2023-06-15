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

import build.buf.validate.Violation;
import org.projectnessie.cel.Program;
import org.projectnessie.cel.common.types.ref.Val;
import org.projectnessie.cel.interpreter.Activation;

class CompiledProgram {
    public final Program program;
    private final Expression source;

    public CompiledProgram(Program program, Expression source) {
        this.program = program;
        this.source = source;
    }

    public Violation eval(Activation bindings) {
        // TODO: work out what to do here
        // now := nowPool.Get()
        // defer nowPool.Put(now)
        // bindings.Next = now

        Program.EvalResult evalResult = program.eval(bindings);
        Val val = evalResult.getVal();
        Object value = val.value();
        if (value instanceof String) {
            if ("".equals(value)) {
                return null;
            }
            return Violation.newBuilder()
                    .setConstraintId(this.source.id)
                    .setMessage(value.toString())
                    .build();
        } else if (value instanceof Boolean) {
            if (val.booleanValue()) {
                return null;
            }
            return Violation.newBuilder()
                    .setConstraintId(this.source.id)
                    .setMessage(this.source.message)
                    .build();
        } else {
            throw new RuntimeException("resolved to an unexpected type " + val);
        }
    }
}