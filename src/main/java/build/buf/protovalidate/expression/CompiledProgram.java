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
import build.buf.gen.buf.validate.priv.Constraint;
import build.buf.gen.buf.validate.priv.PrivateProto;
import build.buf.protovalidate.results.CompilationException;
import build.buf.protovalidate.results.ExecutionException;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import org.projectnessie.cel.Ast;
import org.projectnessie.cel.Env;
import org.projectnessie.cel.EvalOption;
import org.projectnessie.cel.Program;
import org.projectnessie.cel.ProgramOption;
import org.projectnessie.cel.common.types.Err;
import org.projectnessie.cel.common.types.ref.Val;
import org.projectnessie.cel.interpreter.Activation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.projectnessie.cel.ProgramOption.globals;

/**
 * CompiledProgram is a parsed and type-checked {@link Program} along with the source {@link Expression}.
 */
public class CompiledProgram {
    private static final ConcurrentMap<Descriptors.FieldDescriptor, List<CompiledProgramBuilder>> descriptorMap = new ConcurrentHashMap<>();
    private final Env env;
    private final Program program;
    private final Expression source;

    CompiledProgram(Env env, Program program, Expression source) {
        this.env = env;
        this.program = program;
        this.source = source;
    }

    public static List<CompiledProgram> compile(Env env, Message message) throws CompilationException {
        List<CompiledProgramBuilder> programBuilders = getCompiledProgramBuilders(env, message);
        ProgramOption rulesOption = globals(Variable.newRulesVariable(message));
        ProgramOption evalOptions = ProgramOption.evalOptions(
                EvalOption.OptTrackState,
                EvalOption.OptExhaustiveEval,
                EvalOption.OptOptimize,
                EvalOption.OptPartialEval
        );
        List<CompiledProgramBuilder> reducedPrograms = reduce(env, programBuilders, rulesOption, evalOptions);
        if (reducedPrograms.isEmpty()) {
            return null;
        }
        List<CompiledProgram> programs = new ArrayList<>();
        for (CompiledProgramBuilder ast : reducedPrograms) {
            CompiledProgram compiledProgram = ast.build(rulesOption);
            programs.add(compiledProgram);
        }
        return programs;
    }

    public Violation eval(Variable bindings) throws ExecutionException {
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

    private static List<CompiledProgramBuilder> getCompiledProgramBuilders(Env env, Message message) throws CompilationException {
        List<CompiledProgramBuilder> completeProgramList = new ArrayList<>();
        for (Map.Entry<Descriptors.FieldDescriptor, Object> entry : message.getAllFields().entrySet()) {
            Descriptors.FieldDescriptor constraintFieldDesc = entry.getKey();
            if (!descriptorMap.containsKey(constraintFieldDesc)) {
                build.buf.gen.buf.validate.priv.FieldConstraints constraints = constraintFieldDesc.getOptions().getExtension(PrivateProto.field);
                List<CompiledProgramBuilder> programList = compileAsts(constraints.getCelList(), env);
                descriptorMap.put(constraintFieldDesc, programList);
            }
            List<CompiledProgramBuilder> programList = descriptorMap.get(constraintFieldDesc);
            completeProgramList.addAll(programList);
        }
        return completeProgramList;
    }

    /**
     * Returns a reduced {@link Ast} if the expression is statically known to be true or false.
     */
    private Ast reduce(Ast ast) {
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

    /**
     * Compiles the given constraints into a CompiledAstSet.
     */
    private static List<CompiledProgramBuilder> compileAsts(List<Constraint> constraints, Env env) throws CompilationException {
        List<Expression> expressions = Expression.fromPrivConstraints(constraints);
        List<CompiledProgramBuilder> compiledProgramBuilders = new ArrayList<>();
        for (Expression expression : expressions) {
            compiledProgramBuilders.add(CompiledProgramBuilder.newBuilder(env, expression));
        }
        return compiledProgramBuilders;
    }

    private static List<CompiledProgramBuilder> reduce(Env env, List<CompiledProgramBuilder> asts, ProgramOption ...rulesOption) {
        List<CompiledProgramBuilder> residuals = new ArrayList<>();
        for (CompiledProgramBuilder ast : asts) {
            CompiledProgram compiledProgram = ast.build(rulesOption);
            if (compiledProgram == null) {
                residuals.add(ast);
                continue;
            }
            try {
                Ast residual = compiledProgram.reduce(ast.ast);
                if (residual == null) {
                    continue;
                }
                residuals.add(new CompiledProgramBuilder(env, residual, ast.source));
            } catch (Exception e) {
                residuals.add(ast);
            }
        }
        return residuals;
    }
}
