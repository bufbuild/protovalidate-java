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
import build.buf.protovalidate.results.ExecutionException;
import build.buf.protovalidate.results.ValidationResult;
import build.buf.gen.buf.validate.Constraint;
import build.buf.gen.buf.validate.Violation;
import com.google.protobuf.Message;
import org.projectnessie.cel.Env;
import org.projectnessie.cel.EnvOption;

import java.util.ArrayList;
import java.util.List;

/**
 * CompiledProgramSet is a list of {@link CompiledProgram} expressions that are evaluated
 * together with the same input value. All expressions in a CompiledProgramSet may refer
 * to a `this` variable.
 */
public class CompiledProgramSet {

    public final List<CompiledProgram> programs;

    CompiledProgramSet(List<CompiledProgram> programs) {
        this.programs = programs;
    }

    public static CompiledProgramSet compileConstraints(List<Constraint> constraints, Env env, EnvOption... envOpts) throws CompilationException {
        List<Expression> expressions = new ArrayList<>();
        for (Constraint constraint : constraints) {
            expressions.add(new Expression(constraint));
        }
        return compileExpressions(expressions, env, envOpts);
    }

    public static CompiledProgramSet compileExpressions(List<Expression> expressions, Env env, EnvOption... envOpts) throws CompilationException {
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
            CompiledAst compiledAst = CompiledAst.compile(finalEnv, expression);
            CompiledProgram compiledProgram = compiledAst.toCompiledProgram();
            programs.add(compiledProgram);
        }
        return new CompiledProgramSet(programs);
    }

    public ValidationResult evalMessage(Message val, boolean failFast) throws ExecutionException {
        return evalValue(val, failFast);
    }

    public ValidationResult evalValue(Object value, boolean failFast) throws ExecutionException {
        Variable activation = Variable.newThisVariable(value);
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
