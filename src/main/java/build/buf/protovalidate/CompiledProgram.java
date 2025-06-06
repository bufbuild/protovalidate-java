// Copyright 2023-2024 Buf Technologies, Inc.
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

package build.buf.protovalidate;

import build.buf.protovalidate.exceptions.ExecutionException;
import build.buf.validate.FieldPath;
import dev.cel.runtime.CelEvaluationException;
import dev.cel.runtime.CelRuntime.Program;
import dev.cel.runtime.CelVariableResolver;
import org.jspecify.annotations.Nullable;

/**
 * {@link CompiledProgram} is a parsed and type-checked {@link Program} along with the source {@link
 * Expression}.
 */
class CompiledProgram {
  /** A compiled CEL program that can be evaluated against a set of variable bindings. */
  private final Program program;

  /** The original expression that was compiled into the program from the proto file. */
  private final Expression source;

  /** The field path from FieldRules to the rule value. */
  @Nullable private final FieldPath rulePath;

  /** The rule value. */
  @Nullable private final Value ruleValue;

  /**
   * Global variables to pass to the evaluation step. Program/CelRuntime doesn't have a concept of
   * global variables.
   */
  @Nullable private final CelVariableResolver globals;

  /**
   * Constructs a new {@link CompiledProgram}.
   *
   * @param program The compiled CEL program.
   * @param source The original expression that was compiled into the program.
   * @param rulePath The field path from the FieldRules to the rule value.
   * @param ruleValue The rule value.
   */
  public CompiledProgram(
      Program program,
      Expression source,
      @Nullable FieldPath rulePath,
      @Nullable Value ruleValue,
      @Nullable CelVariableResolver globals) {
    this.program = program;
    this.source = source;
    this.rulePath = rulePath;
    this.ruleValue = ruleValue;
    this.globals = globals;
  }

  /**
   * Evaluate the compiled program with a given set of {@link Variable} variables.
   *
   * @param variables Variable variables used for the evaluation.
   * @param fieldValue Field value to return in violations.
   * @return The {@link build.buf.validate.Violation} from the evaluation, or null if there are no
   *     violations.
   * @throws ExecutionException If the evaluation of the CEL program fails with an error.
   */
  public RuleViolation.@Nullable Builder eval(Value fieldValue, CelVariableResolver variables)
      throws ExecutionException {
    Object value;
    try {
      if (this.globals != null) {
        variables = CelVariableResolver.hierarchicalVariableResolver(variables, this.globals);
      }
      value = program.eval(variables);
    } catch (CelEvaluationException e) {
      throw new ExecutionException(String.format("error evaluating %s: %s", source.id, e));
    }
    if (value instanceof String) {
      if ("".equals(value)) {
        return null;
      }
      RuleViolation.Builder builder =
          RuleViolation.newBuilder().setRuleId(this.source.id).setMessage(value.toString());
      if (fieldValue.fieldDescriptor() != null) {
        builder.setFieldValue(new RuleViolation.FieldValue(fieldValue));
      }
      if (rulePath != null) {
        builder.addAllRulePathElements(rulePath.getElementsList());
      }
      if (ruleValue != null && ruleValue.fieldDescriptor() != null) {
        builder.setRuleValue(new RuleViolation.FieldValue(ruleValue));
      }
      return builder;
    } else if (value instanceof Boolean) {
      if (Boolean.TRUE.equals(value)) {
        return null;
      }
      RuleViolation.Builder builder =
          RuleViolation.newBuilder().setRuleId(this.source.id).setMessage(this.source.message);
      if (rulePath != null) {
        builder.addAllRulePathElements(rulePath.getElementsList());
      }
      if (ruleValue != null && ruleValue.fieldDescriptor() != null) {
        builder.setRuleValue(new RuleViolation.FieldValue(ruleValue));
      }
      return builder;
    } else {
      throw new ExecutionException(String.format("resolved to an unexpected type %s", value));
    }
  }
}
