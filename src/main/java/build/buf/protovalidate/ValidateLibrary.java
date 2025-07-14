// Copyright 2023-2025 Buf Technologies, Inc.
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

import dev.cel.checker.CelCheckerBuilder;
import dev.cel.common.CelVarDecl;
import dev.cel.common.types.SimpleType;
import dev.cel.compiler.CelCompilerLibrary;
import dev.cel.parser.CelParserBuilder;
import dev.cel.parser.CelStandardMacro;
import dev.cel.runtime.CelRuntimeBuilder;
import dev.cel.runtime.CelRuntimeLibrary;

/**
 * Custom {@link CelCompilerLibrary} and {@link CelRuntimeLibrary}. Provides all the custom
 * extension function definitions and overloads.
 */
final class ValidateLibrary implements CelCompilerLibrary, CelRuntimeLibrary {

  /** Creates a ValidateLibrary with all custom declarations and overloads. */
  ValidateLibrary() {}

  @Override
  public void setParserOptions(CelParserBuilder parserBuilder) {
    parserBuilder.setStandardMacros(
        CelStandardMacro.ALL,
        CelStandardMacro.EXISTS,
        CelStandardMacro.EXISTS_ONE,
        CelStandardMacro.FILTER,
        CelStandardMacro.HAS,
        CelStandardMacro.MAP,
        CelStandardMacro.MAP_FILTER);
  }

  @Override
  public void setCheckerOptions(CelCheckerBuilder checkerBuilder) {
    checkerBuilder
        .addVarDeclarations(
            CelVarDecl.newVarDeclaration(NowVariable.NOW_NAME, SimpleType.TIMESTAMP))
        .addFunctionDeclarations(CustomDeclarations.create());
  }

  @Override
  public void setRuntimeOptions(CelRuntimeBuilder runtimeBuilder) {
    runtimeBuilder.addFunctionBindings(CustomOverload.create());
  }
}
