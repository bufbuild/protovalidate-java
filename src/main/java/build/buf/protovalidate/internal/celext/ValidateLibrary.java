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

package build.buf.protovalidate.internal.celext;

import build.buf.protovalidate.internal.expression.NowVariable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.projectnessie.cel.EnvOption;
import org.projectnessie.cel.EvalOption;
import org.projectnessie.cel.Library;
import org.projectnessie.cel.ProgramOption;

/**
 * Custom {@link Library} for CEL. Provides all the custom extension function definitions and
 * overloads.
 */
public class ValidateLibrary implements Library {

  /** Creates a new ValidateLibrary, with all custom declarations and overloads. */
  public ValidateLibrary() {}

  /**
   * Returns the compile options for the CEL environment.
   *
   * @return the compile options.
   */
  @Override
  public List<EnvOption> getCompileOptions() {
    return Collections.singletonList(EnvOption.declarations(CustomDeclarations.create()));
  }

  /**
   * Returns the program options for the CEL program.
   *
   * @return the program options.
   */
  @Override
  public List<ProgramOption> getProgramOptions() {
    return Arrays.asList(
        ProgramOption.evalOptions(EvalOption.OptOptimize),
        ProgramOption.globals(new NowVariable()),
        ProgramOption.functions(CustomOverload.create()));
  }
}
