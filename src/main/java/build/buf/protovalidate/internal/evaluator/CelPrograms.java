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

package build.buf.protovalidate.internal.evaluator;

import build.buf.protovalidate.exceptions.ExecutionException;
import build.buf.protovalidate.internal.errors.ConstraintViolation;
import build.buf.protovalidate.internal.errors.FieldPathUtils;
import build.buf.protovalidate.internal.expression.CompiledProgram;
import build.buf.protovalidate.internal.expression.Variable;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/** Evaluator that executes a {@link CompiledProgram}. */
class CelPrograms implements Evaluator {
  private final ConstraintViolationHelper helper;

  /** A list of {@link CompiledProgram} that will be executed against the input message. */
  private final List<CompiledProgram> programs;

  /**
   * Constructs a new {@link CelPrograms}.
   *
   * @param compiledPrograms The programs to execute.
   */
  CelPrograms(@Nullable ValueEvaluator valueEvaluator, List<CompiledProgram> compiledPrograms) {
    this.helper = new ConstraintViolationHelper(valueEvaluator);
    this.programs = compiledPrograms;
  }

  @Override
  public boolean tautology() {
    return programs.isEmpty();
  }

  @Override
  public List<ConstraintViolation.Builder> evaluate(Value val, boolean failFast)
      throws ExecutionException {
    Variable activation = Variable.newThisVariable(val.value(Object.class));
    List<ConstraintViolation.Builder> violations = new ArrayList<>();
    for (CompiledProgram program : programs) {
      ConstraintViolation.Builder violation = program.eval(val, activation);
      if (violation != null) {
        violations.add(violation);
        if (failFast) {
          break;
        }
      }
    }
    return FieldPathUtils.updatePaths(
        violations, helper.getFieldPathElement(), helper.getRulePrefixElements());
  }
}
