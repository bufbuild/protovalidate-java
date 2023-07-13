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

package build.buf.protovalidate.constraints;

import static org.projectnessie.cel.ProgramOption.globals;

import build.buf.gen.buf.validate.FieldConstraints;
import build.buf.gen.buf.validate.priv.PrivateProto;
import build.buf.protovalidate.expression.AstExpression;
import build.buf.protovalidate.expression.CompiledProgram;
import build.buf.protovalidate.expression.Expression;
import build.buf.protovalidate.expression.Variable;
import build.buf.protovalidate.results.CompilationException;
import com.google.api.expr.v1alpha1.Type;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.projectnessie.cel.Ast;
import org.projectnessie.cel.Env;
import org.projectnessie.cel.EnvOption;
import org.projectnessie.cel.EvalOption;
import org.projectnessie.cel.Program;
import org.projectnessie.cel.ProgramOption;
import org.projectnessie.cel.checker.Decls;
import org.projectnessie.cel.common.types.ref.Val;
import org.projectnessie.cel.interpreter.Activation;

/** {@link ConstraintCache} is a build-through cache to computed standard constraints. */
public class ConstraintCache {
  private static final ProgramOption PARTIAL_EVAL_OPTIONS =
      ProgramOption.evalOptions(
          EvalOption.OptTrackState,
          EvalOption.OptExhaustiveEval,
          EvalOption.OptOptimize,
          EvalOption.OptPartialEval);
  private static final ConcurrentMap<FieldDescriptor, List<AstExpression>> descriptorMap =
      new ConcurrentHashMap<>();

  private final Env env;

  /** Constructs a new build-through cache for the standard constraints. */
  public ConstraintCache(Env env) {
    this.env = env;
  }

  /**
   * Creates the standard constraints for the given field. If forItems is true, the constraints for
   * repeated list items is built instead of the constraints on the list itself.
   */
  public List<CompiledProgram> compile(
      FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, Boolean forItems)
      throws CompilationException {
    Message message = resolveConstraints(fieldDescriptor, fieldConstraints, forItems);
    if (message == null) {
      // Message null means there were no constraints resolved.
      return null;
    }
    Env finalEnv =
        env.extend(
            EnvOption.types(message.getDefaultInstanceForType()),
            EnvOption.declarations(
                Decls.newVar(Variable.THIS_NAME, getCELType(fieldDescriptor, forItems)),
                Decls.newVar(
                    Variable.RULES_NAME,
                    Decls.newObjectType(message.getDescriptorForType().getFullName()))));
    ProgramOption rulesOption = globals(Variable.newRulesVariable(message));
    List<AstExpression> completeProgramList = new ArrayList<>();
    for (Map.Entry<FieldDescriptor, Object> entry : message.getAllFields().entrySet()) {
      FieldDescriptor constraintFieldDesc = entry.getKey();
      if (!descriptorMap.containsKey(constraintFieldDesc)) {
        build.buf.gen.buf.validate.priv.FieldConstraints constraints =
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
        AstExpression residualAstExpression = new AstExpression(residual, astExpression.source);

        programs.add(
            new CompiledProgram(
                finalEnv.program(residualAstExpression.ast, rulesOption),
                residualAstExpression.source));
      } catch (Exception e) {
        programs.add(
            new CompiledProgram(
                finalEnv.program(astExpression.ast, rulesOption), astExpression.source));
      }
    }
    return programs;
  }

  /**
   * Extracts the standard constraints for the specified field. An exception is thrown if the wrong
   * constraints are applied to a field (typically if there is a type-mismatch). Null is returned if
   * there are no standard constraints to apply to this field.
   */
  private Message resolveConstraints(
      FieldDescriptor fieldDescriptor, FieldConstraints fieldConstraints, Boolean forItems)
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
        getExpectedConstraintDescriptor(fieldDescriptor, forItems);
    boolean ok = expectedConstraintDescriptor != null;
    if (ok
        && !oneofFieldDescriptor.getFullName().equals(expectedConstraintDescriptor.getFullName())) {
      // If the expected constraint does not match the actual oneof constraint, throw a
      // CompilationError.
      throw new CompilationException(
          "expected constraint %s, got %s on field %s",
          expectedConstraintDescriptor.getName(),
          oneofFieldDescriptor.getName(),
          fieldDescriptor.getName());
    }

    // If the expected constraint descriptor is null or if the field constraints do not have the
    // oneof field descriptor
    // there are no constraints to resolve, so return null.
    if (!ok || !fieldConstraints.hasField(oneofFieldDescriptor)) {
      return null;
    }

    // Return the field from the field constraints identified by the oneof field descriptor, casted
    // as a Message.
    return (Message) fieldConstraints.getField(oneofFieldDescriptor);
  }

  /**
   * Produces the field descriptor from the {@link FieldConstraints} 'type' oneof that matches the
   * provided target field descriptor. If the returned value is null, the field does not expect any
   * standard constraints.
   */
  private FieldDescriptor getExpectedConstraintDescriptor(
      FieldDescriptor fieldDescriptor, Boolean forItems) {
    if (fieldDescriptor.isMapField()) {
      return DescriptorMappings.MAP_FIELD_CONSTRAINTS_DESC;
    } else if (fieldDescriptor.isRepeated() && !forItems) {
      return DescriptorMappings.REPEATED_FIELD_CONSTRAINTS_DESC;
    } else if (fieldDescriptor.getJavaType() == FieldDescriptor.JavaType.MESSAGE) {
      return DescriptorMappings.EXPECTED_WKT_CONSTRAINTS.get(
          fieldDescriptor.getMessageType().getFullName());
    } else {
      return DescriptorMappings.EXPECTED_STANDARD_CONSTRAINTS.get(fieldDescriptor.getType());
    }
  }

  /**
   * Resolves the CEL value type for the provided {@link FieldDescriptor}. If forItems is true, the
   * type for the repeated list items is returned instead of the list type itself.
   */
  private Type getCELType(FieldDescriptor fieldDescriptor, Boolean forItems) {
    if (!forItems) {
      if (fieldDescriptor.isMapField()) {
        return Decls.newMapType(
            getCELType(fieldDescriptor.getMessageType().findFieldByNumber(1), true),
            getCELType(fieldDescriptor.getMessageType().findFieldByNumber(2), true));
      } else if (fieldDescriptor.isRepeated()) {
        return Decls.newListType(getCELType(fieldDescriptor, true));
      }
    }

    if (fieldDescriptor.getType() == FieldDescriptor.Type.MESSAGE) {
      String fqn = fieldDescriptor.getMessageType().getFullName();
      switch (fqn) {
        case "google.protobuf.Any":
          return Decls.newWellKnownType(Type.WellKnownType.ANY);
        case "google.protobuf.Duration":
          return Decls.newWellKnownType(Type.WellKnownType.DURATION);
        case "google.protobuf.Timestamp":
          return Decls.newWellKnownType(Type.WellKnownType.TIMESTAMP);
        default:
          return Decls.newObjectType(fieldDescriptor.getFullName());
      }
    }
    return DescriptorMappings.protoKindToCELType(fieldDescriptor.getType());
  }
}
