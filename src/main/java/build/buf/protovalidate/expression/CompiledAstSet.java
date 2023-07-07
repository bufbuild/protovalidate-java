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

import build.buf.protovalidate.results.CompilationException;
import org.projectnessie.cel.Ast;
import org.projectnessie.cel.Env;
import org.projectnessie.cel.EnvOption;
import org.projectnessie.cel.EvalOption;
import org.projectnessie.cel.ProgramOption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// AstSet represents a collection of CompiledAst and their associated CelRuntime.
public class CompiledAstSet {
    public final List<CompiledAst> asts;
    public final Env env;

    public CompiledAstSet(Env env, List<CompiledAst> asts) {
        this.env = env;
        this.asts = asts;
    }

    public static CompiledAstSet compileAsts(List<build.buf.gen.buf.validate.priv.Constraint> constraints, Env env, EnvOption... envOpts) throws CompilationException {
        List<Expression> expressions = new ArrayList<>();
        for (build.buf.gen.buf.validate.priv.Constraint constraint : constraints) {
            expressions.add(new Expression(constraint));
        }
        Env finalEnv = env;
        if (envOpts.length > 0) {
            finalEnv = env.extend(envOpts);
        }
        List<CompiledAst> compiledAsts = new ArrayList<>();
        for (Expression expression : expressions) {
            compiledAsts.add(CompiledAst.compile(env, expression));
        }
        return new CompiledAstSet(finalEnv, compiledAsts);
    }

    public void set(int index, CompiledAst ast) {
        asts.add(index, ast);
    }

    public void merge(CompiledAstSet other) {
        asts.addAll(other.asts);
    }

    // ReduceResiduals generates a ProgramSet, performing a partial evaluation of
    // the AstSet to optimize the expression. If the expression is optimized to
    // either a true or empty string constant result, no CompiledProgram is
    // generated for it. The main usage of this is to elide tautological expressions
    // from the final result.
    public CompiledProgramSet reduceResiduals(ProgramOption... opts) {
        CompiledAstSet compiledAstSet = reduce(opts);
        return compiledAstSet.toProgramSet(opts);
    }

    // ToProgramSet generates a ProgramSet from the specified ASTs.
    public CompiledProgramSet toProgramSet(ProgramOption... opts) {
        if (asts.isEmpty()) {
            return null;
        }
        List<CompiledProgram> programs = new ArrayList<>();
        for (CompiledAst ast : asts) {
            CompiledProgram compiledProgram = ast.toCompiledProgram(opts);
            programs.add(compiledProgram);
        }
        return new CompiledProgramSet(programs);
    }

    private CompiledAstSet reduce(ProgramOption... opts) {
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
            CompiledProgram compiledProgram = ast.toCompiledProgram(options.toArray(new ProgramOption[0]));
            if (compiledProgram == null) {
                residuals.add(ast);
                continue;
            }
            try {
                Ast residual = compiledProgram.reduce(ast.ast);
                if (residual == null) {
                    continue;
                }
                residuals.add(new CompiledAst(env, residual, ast.source));
            } catch (Exception e) {
                residuals.add(ast);
            }
        }
        return new CompiledAstSet(env, residuals);
    }
}
