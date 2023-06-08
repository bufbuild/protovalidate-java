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
import org.projectnessie.cel.Ast;
import org.projectnessie.cel.Env;
import org.projectnessie.cel.EvalDetails;
import org.projectnessie.cel.EvalOption;
import org.projectnessie.cel.ProgramOption;
import org.projectnessie.cel.interpreter.Activation;
import org.projectnessie.cel.interpreter.EvalState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// AstSet represents a collection of CompiledAst and their associated CelRuntime.
public class AstSet {
    private final Env env;
    private List<CompiledAst> asts;

    public AstSet(Env env, List<CompiledAst> asts) {
        this.env = env;
        this.asts = asts;
    }

    // Merge combines a set with another, producing a new AstSet.
    public AstSet merge(AstSet other) {
        List<CompiledAst> mergedList = new ArrayList<>(asts);
        mergedList.addAll(other.asts);
        return new AstSet(env, mergedList);
    }

    // ReduceResiduals generates a ProgramSet, performing a partial evaluation of
    // the AstSet to optimize the expression. If the expression is optimized to
    // either a true or empty string constant result, no CompiledProgram is
    // generated for it. The main usage of this is to elide tautological expressions
    // from the final result.
    public ProgramSet reduceResiduals(ProgramOption... opts) {
        ProgramOption programOption = ProgramOption.evalOptions(
                EvalOption.OptTrackState,
                EvalOption.OptExhaustiveEval,
                EvalOption.OptOptimize,
                EvalOption.OptPartialEval
        );
        List<ProgramOption> options = new ArrayList<>();
        options.add(programOption);
        options.addAll(Arrays.asList(opts));
        List<CompiledAst> residuals = new ArrayList<>();
        for (CompiledAst ast : asts) {
            CompiledProgram compiledProgram = ast.toProgram(env, options.toArray(new ProgramOption[0]));
            if (compiledProgram == null) {
                residuals.add(ast);
                continue;
            }
            Violation violation = compiledProgram.eval(Activation.emptyActivation());
            if (violation != null) {
                // TODO
                continue;
            }
            // TODO: get the eval state from the eval method.
            Ast residualAst = env.residualAst(ast.ast, new EvalDetails(EvalState.newEvalState()));
            if (residualAst == null) {
                // TODO:
                residuals.add(ast);
                continue;
            }
            residuals.add(new CompiledAst(residualAst, ast.source));
        }
        return new AstSet(
                env,
                residuals
        ).toProgramSet(opts);
    }

    // ToProgramSet generates a ProgramSet from the specified ASTs.
    public ProgramSet toProgramSet(ProgramOption... opts) {
        if (asts.isEmpty()) {
            return null;
        }
        List<CompiledProgram> programs = new ArrayList<>();
        for (CompiledAst ast : asts) {
            CompiledProgram compiledProgram = ast.toProgram(env, opts);
            programs.add(compiledProgram);
        }
        return new ProgramSet(programs);
    }
}