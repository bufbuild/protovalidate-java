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

package build.buf.protovalidate.internal.constraints;

import build.buf.protovalidate.exceptions.CompilationException;
import build.buf.protovalidate.internal.expression.AstExpression;
import build.buf.protovalidate.internal.expression.CompiledProgram;
import build.buf.protovalidate.internal.expression.Expression;
import build.buf.protovalidate.internal.expression.Variable;
import build.buf.validate.FieldConstraints;
import build.buf.validate.priv.PrivateProto;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import org.projectnessie.cel.Ast;
import org.projectnessie.cel.Env;
import org.projectnessie.cel.EnvOption;
import org.projectnessie.cel.EvalOption;
import org.projectnessie.cel.Program;
import org.projectnessie.cel.ProgramOption;
import org.projectnessie.cel.checker.Decls;
import org.projectnessie.cel.common.types.ref.Val;
import org.projectnessie.cel.interpreter.Activation;

/** A build-through cache for computed standard constraints. */
public class ConstraintCache {
  /** Partial eval options for evaluating the constraint's expression. */
  private static final ProgramOption PARTIAL_EVAL_OPTIONS =
      ProgramOption.evalOptions(
          EvalOption.OptTrackState,
          EvalOption.OptExhaustiveEval,
          EvalOption.OptOptimize,
          EvalOption.OptPartialEval);

  /**
   * Concurrent map for caching {@link FieldDescriptor} and their associated List of {@link
   * AstExpression}.
   */
  private static final Map<FieldDescriptor, List<AstExpression>> descriptorMap =
      new ConcurrentHashMap<>();

  /** The environment to use for evaluation. */
  private final Env env;

  /** Constructs a new build-through cache for the standard constraints. */
  public ConstraintCache(Env env) {
    this.env = env;
  }

  /**
   * Creates the standard constraints for the given field. If forItems is true, the constraints for
   * repeated list items is built instead of the constraints on the list itself.
   *
   * @param fieldDescriptor The field descriptor to be validated.
   * @param fieldConstraints The field constraint that is used for validation.
   * @param forItems The field is an item list type.
   * @return The list of compiled programs.
   * @throws CompilationException If the constraints fail to compile.
   */
  public List<CompiledProgram> compile(
      FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, boolean forItems)
      throws CompilationException {
    Message message = resolveConstraints(fieldDescriptor, fieldConstraints, forItems);
    if (message == null) {
      // Message null means there were no constraints resolved.
      return Collections.emptyList();
    }
    Env finalEnv =
        env.extend(
            EnvOption.types(message.getDefaultInstanceForType()),
            EnvOption.declarations(
                Decls.newVar(
                    Variable.THIS_NAME, DescriptorMappings.getCELType(fieldDescriptor, forItems)),
                Decls.newVar(
                    Variable.RULES_NAME,
                    Decls.newObjectType(message.getDescriptorForType().getFullName()))));
    ProgramOption rulesOption = ProgramOption.globals(Variable.newRulesVariable(message));
    List<AstExpression> completeProgramList = new ArrayList<>();
    for (Map.Entry<FieldDescriptor, Object> entry : message.getAllFields().entrySet()) {
      FieldDescriptor constraintFieldDesc = entry.getKey();
      if (!descriptorMap.containsKey(constraintFieldDesc)) {
        build.buf.validate.priv.FieldConstraints constraints =
            constraintFieldDesc.getOptions().getExtension(PrivateProto.field);
        List<Expression> expressions = Expression.fromPrivConstraints(constraints.getCelList());
        List<AstExpression> astExpressions = new ArrayList<>();
        for (Expression expression : expressions) {
          astExpressions.add(AstExpression.newAstExpression(finalEnv, expression));
        }
        descriptorMap.put(constraintFieldDesc, astExpressions);
      }
      List<AstExpression> programList = descriptorMap.get(constraintFieldDesc);
      completeProgramList.addAll(programList);
    }
    List<CompiledProgram> programs = new ArrayList<>();
    for (AstExpression astExpression : completeProgramList) {
      try {
        Program program = finalEnv.program(astExpression.ast, rulesOption, PARTIAL_EVAL_OPTIONS);
        Program.EvalResult evalResult = program.eval(Activation.emptyActivation());
        Val value = evalResult.getVal();
        if (value != null) {
          Object val = value.value();
          if (val instanceof Boolean && value.booleanValue()) {
            continue;
          }
          if (val instanceof String && val.equals("")) {
            continue;
          }
        }
        Ast residual = finalEnv.residualAst(astExpression.ast, evalResult.getEvalDetails());
        programs.add(
            new CompiledProgram(finalEnv.program(residual, rulesOption), astExpression.source));
      } catch (Exception e) {
        programs.add(
            new CompiledProgram(
                finalEnv.program(astExpression.ast, rulesOption), astExpression.source));
      }
    }
    return Collections.unmodifiableList(programs);
  }

  /**
   * Extracts the standard constraints for the specified field. An exception is thrown if the wrong
   * constraints are applied to a field (typically if there is a type-mismatch). Null is returned if
   * there are no standard constraints to apply to this field.
   */
  @Nullable
  private Message resolveConstraints(
      FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, boolean forItems)
      throws CompilationException {
    // Get the oneof field descriptor from the field constraints.
    FieldDescriptor oneofFieldDescriptor =
        fieldConstraints.getOneofFieldDescriptor(DescriptorMappings.FIELD_CONSTRAINTS_ONEOF_DESC);
    if (oneofFieldDescriptor == null) {
      // If the oneof field descriptor is null there are no constraints to resolve.
      return null;
    }

    // Get the expected constraint descriptor based on the provided field descriptor and the flag
    // indicating whether it is for items.
    FieldDescriptor expectedConstraintDescriptor =
        DescriptorMappings.getExpectedConstraintDescriptor(fieldDescriptor, forItems);
    if (expectedConstraintDescriptor != null
        && !oneofFieldDescriptor.getFullName().equals(expectedConstraintDescriptor.getFullName())) {
      // If the expected constraint does not match the actual oneof constraint, throw a
      // CompilationError.
      throw new CompilationException(
          String.format(
              "expected constraint %s, got %s on field %s",
              expectedConstraintDescriptor.getName(),
              oneofFieldDescriptor.getName(),
              fieldDescriptor.getName()));
    }

    // If the expected constraint descriptor is null or if the field constraints do not have the
    // oneof field descriptor
    // there are no constraints to resolve, so return null.
    if (expectedConstraintDescriptor == null || !fieldConstraints.hasField(oneofFieldDescriptor)) {
      return null;
    }

    // Return the field from the field constraints identified by the oneof field descriptor, casted
    // as a Message.
    return (Message) fieldConstraints.getField(oneofFieldDescriptor);
  }
}
