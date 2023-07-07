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

import build.buf.gen.buf.validate.Violation;
import build.buf.protovalidate.results.ExecutionException;
import org.projectnessie.cel.Ast;
import org.projectnessie.cel.Env;
import org.projectnessie.cel.Program;
import org.projectnessie.cel.common.types.Err;
import org.projectnessie.cel.common.types.ref.Val;
import org.projectnessie.cel.interpreter.Activation;

/**
 * CompiledProgram is a parsed and type-checked {@link Program} along with the source {@link Expression}.
 */
class CompiledProgram {
    private final Env env;
    private final Program program;
    private final Expression source;

    CompiledProgram(Env env, Program program, Expression source) {
        this.env = env;
        this.program = program;
        this.source = source;
    }

    /**
     * Returns a reduced {@link Ast} if the expression is statically known to be true or false.
     */
    Ast reduce(Ast ast) {
        Program.EvalResult evalResult = program.eval(Activation.emptyActivation());
        Val value = evalResult.getVal();
        if (value != null) {
            Object val = value.value();
            if (val instanceof Boolean && value.booleanValue()) {
                return null;
            }
            if (val instanceof String && val.equals("")) {
                return null;
            }
        }
        return env.residualAst(ast, evalResult.getEvalDetails());
    }

    Violation eval(Activation bindings) throws ExecutionException {
        Program.EvalResult evalResult = program.eval(bindings);
        Val val = evalResult.getVal();
        if (val instanceof Err) {
            throw new ExecutionException("error evaluating %s: %s", source.id, val.toString());
        }
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
            throw new ExecutionException("resolved to an unexpected type %s", val);
        }
    }
}