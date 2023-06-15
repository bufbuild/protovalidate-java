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
import org.projectnessie.cel.*;
import org.projectnessie.cel.common.types.ref.Val;
import org.projectnessie.cel.interpreter.Activation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// AstSet represents a collection of CompiledAst and their associated CelRuntime.
public class AstSet {
    public final List<CompiledAst> asts;
    public final Env env;

    public AstSet(Env env, List<CompiledAst> asts) {
        this.env = env;
        this.asts = asts;
    }

    public void set(int index, CompiledAst ast) {
        asts.add(index, ast);
    }

    public void merge(AstSet other) {
        asts.addAll(other.asts);
    }

    // ReduceResiduals generates a ProgramSet, performing a partial evaluation of
    // the AstSet to optimize the expression. If the expression is optimized to
    // either a true or empty string constant result, no CompiledProgram is
    // generated for it. The main usage of this is to elide tautological expressions
    // from the final result.
    public ProgramSet reduceResiduals(ProgramOption... opts) {
        AstSet astSet = reduce(opts);
        ProgramSet programSet = astSet.toProgramSet(opts);
        return programSet;
    }

    private AstSet reduce(ProgramOption... opts) {
        List<CompiledAst> residuals = new ArrayList<>();
        List<ProgramOption> options = new ArrayList<>();
        options.addAll(Arrays.asList(opts));
        options.add(ProgramOption.evalOptions(
                EvalOption.OptTrackState,
                EvalOption.OptExhaustiveEval,
                EvalOption.OptOptimize,
                EvalOption.OptPartialEval
        ));
        for (CompiledAst ast : asts) {
            CompiledProgram compiledProgram = ast.toCompiledProgram(env, options.toArray(new ProgramOption[0]));
            if (compiledProgram == null) {
                residuals.add(ast);
                continue;
            }
            Program.EvalResult evalResult = compiledProgram.program.eval(Activation.emptyActivation());
            Val value = evalResult.getVal();
            if (value != null) {
                Object val = value.value();
                if (val instanceof Boolean && value.booleanValue()) {
                    continue;
                }
                if (val instanceof String && val.toString().equals("")) {
                    continue;
                }
            }
            try {
                Ast residual = env.residualAst(ast.ast, evalResult.getEvalDetails());
                residuals.add(new CompiledAst(residual, ast.source));
            } catch (Exception e) {
                residuals.add(ast);
            }
        }
        return new AstSet(env, residuals);
    }

    // ToProgramSet generates a ProgramSet from the specified ASTs.
    public ProgramSet toProgramSet(ProgramOption... opts) {
        if (asts.isEmpty()) {
            return null;
        }
        List<CompiledProgram> programs = new ArrayList<>();
        for (CompiledAst ast : asts) {
            CompiledProgram compiledProgram = ast.toCompiledProgram(env, opts);
            programs.add(compiledProgram);
        }
        return new ProgramSet(programs);
    }
}