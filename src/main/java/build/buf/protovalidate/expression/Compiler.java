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

import build.buf.validate.Constraint;
import com.google.api.expr.v1alpha1.Type;
import org.projectnessie.cel.Ast;
import org.projectnessie.cel.Env;
import org.projectnessie.cel.EnvOption;
import org.projectnessie.cel.tools.ScriptCreateException;
import org.projectnessie.cel.Issues;

import java.util.ArrayList;
import java.util.List;

import static build.buf.protovalidate.errors.CompilationError.newCompilationError;

// Compile produces a ProgramSet from the provided expressions in the given
// environment. If the generated cel.Program require cel.ProgramOption params,
// use CompileASTs instead with a subsequent call to AstSet.ToProgramSet.
public class Compiler  {
    public static <T extends Constraint> ProgramSet compile(List<T> expressions, Env env, EnvOption... envOpts) {
        if (expressions.isEmpty()) {
            return null;
        }
        Env finalEnv = env;
        if (envOpts.length > 0) {
            try {
                finalEnv = env.extend(envOpts);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        List<CompiledProgram> programs = new ArrayList<>();
        for (T expression : expressions) {
            CompiledAst compiledAst = compileAST(finalEnv, expression);
            CompiledProgram compiledProgram = compiledAst.toCompiledProgram(finalEnv);
            programs.add(compiledProgram);
        }
        return new ProgramSet(programs);
    }

    // CompileASTs parses and type checks a set of expressions, producing a resulting
    // AstSet. The value can then be converted to a ProgramSet via
    // AstSet.ToProgramSet or AstSet.ReduceResiduals. Use Compile instead if no
    // cel.ProgramOption args need to be provided or residuals do not need to be
    // computed.
    public static <T extends Constraint> AstSet compileASTs(List<T> expressions, Env env, EnvOption... envOpts) {
        AstSet set = new AstSet(env, expressions.size());
        if (expressions.size() == 0) {
            return null;
        }

        if (envOpts.length > 0) {
            Env newenv = env.extend(envOpts);
            set = new AstSet(newenv, set.asts);
        }

        for (int i = 0; i < expressions.size(); i++) {
            Constraint expr = expressions.get(i);
            CompiledAst compiledAst = compileAST(set.env, expr);
            set.set(i, compiledAst);
        }

        return set;
    }

    private static CompiledAst compileAST(Env env, Constraint expr) {
        Env.AstIssuesTuple astIssuesTuple = env.compile(expr.getExpression());
        if (astIssuesTuple.hasIssues()) {
            throw new RuntimeException(new ScriptCreateException("ast error", astIssuesTuple.getIssues()));
        }
        Ast ast = astIssuesTuple.getAst();
        Type outType = ast.getResultType();
        boolean ok = !(outType.equals(Type.PrimitiveType.BOOL) || outType.equals(Type.PrimitiveType.STRING));
        if (!ok) {
            throw newCompilationError(
                    "expression outputs, wanted either bool or string", expr.getId(), outType.toString());
        }

        return new CompiledAst(ast, new Expression(expr));
    }
}