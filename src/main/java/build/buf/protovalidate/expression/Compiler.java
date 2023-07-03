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
import build.buf.validate.Constraint;
import com.google.api.expr.v1alpha1.Type;
import org.projectnessie.cel.Ast;
import org.projectnessie.cel.Env;
import org.projectnessie.cel.EnvOption;
import org.projectnessie.cel.common.Source;

import java.util.ArrayList;
import java.util.List;

public class Compiler {
    public static AstSet compileASTs(List<build.buf.validate.priv.Constraint> constraints, Env env, EnvOption... envOpts) throws CompilationException {
        List<Expression> expressions = new ArrayList<>();
        for (build.buf.validate.priv.Constraint constraint : constraints) {
            expressions.add(new Expression(constraint.getId(), constraint.getMessage(), constraint.getExpression()));
        }
        Env finalEnv = env;
        if (envOpts.length > 0) {
            finalEnv = env.extend(envOpts);
        }
        List<CompiledAst> compiledAsts = new ArrayList<>();
        for (Expression expression : expressions) {
            compiledAsts.add(compileAST(env, expression));
        }
        return new AstSet(finalEnv, compiledAsts);
    }

    public static ProgramSet compileConstraints(List<Constraint> constraints, Env env, EnvOption... envOpts) throws CompilationException {
        List<Expression> expressions = new ArrayList<>();
        for (Constraint constraint : constraints) {
            expressions.add(new Expression(constraint.getId(), constraint.getMessage(), constraint.getExpression()));
        }
        return compile(expressions, env, envOpts);
    }

    public static ProgramSet compile(List<Expression> expressions, Env env, EnvOption... envOpts) throws CompilationException {
        Env finalEnv = env;
        finalEnv.extend(EnvOption.features(EnvOption.EnvFeature.FeatureDisableDynamicAggregateLiterals));
        if (envOpts.length > 0) {
            try {
                finalEnv = env.extend(envOpts);
            } catch (Exception e) {
                throw new CompilationException("failed to extend environment: " + e.getMessage());
            }
        }
        List<CompiledProgram> programs = new ArrayList<>();
        for (Expression expression : expressions) {
            CompiledAst compiledAst = compileAST(finalEnv, expression);
            CompiledProgram compiledProgram = compiledAst.toCompiledProgram(finalEnv);
            programs.add(compiledProgram);
        }
        return new ProgramSet(programs);
    }

    private static CompiledAst compileAST(Env env, Expression expr) throws CompilationException {
        env.parseSource(Source.newTextSource(expr.expression));
        Env.AstIssuesTuple astIssuesTuple = env.compile(expr.expression);
        if (astIssuesTuple.hasIssues()) {
            throw new CompilationException("failed to compile expression " + expr.id);
        }
        Ast ast = astIssuesTuple.getAst();
        Type outType = ast.getResultType();
        // TODO: This is false always. Comparing incompatible types.
        if (outType.equals(Type.PrimitiveType.BOOL) || outType.equals(Type.PrimitiveType.STRING)) {
            throw new CompilationException("expression outputs, wanted either bool or string %s %s", expr.id, outType.toString());
        }
        return new CompiledAst(ast, expr);
    }
}
