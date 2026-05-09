// Copyright 2023-2026 Buf Technologies, Inc.
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

import com.google.re2j.Pattern;
import dev.cel.bundle.Cel;
import dev.cel.bundle.CelFactory;
import dev.cel.checker.CelCheckerBuilder;
import dev.cel.checker.CelStandardDeclarations;
import dev.cel.common.CelOptions;
import dev.cel.common.CelVarDecl;
import dev.cel.common.types.SimpleType;
import dev.cel.compiler.CelCompilerLibrary;
import dev.cel.extensions.CelExtensions;
import dev.cel.parser.CelParserBuilder;
import dev.cel.parser.CelStandardMacro;
import dev.cel.runtime.CelRuntimeBuilder;
import dev.cel.runtime.CelRuntimeLibrary;
import dev.cel.runtime.CelStandardFunctions;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Custom {@link CelCompilerLibrary} and {@link CelRuntimeLibrary}. Provides all the custom
 * extension function definitions and overloads.
 */
final class ValidateLibrary implements CelCompilerLibrary, CelRuntimeLibrary {

  private static final CelOptions CEL_OPTIONS = CelOptions.DEFAULT;

  private final ConcurrentMap<String, Pattern> patternCache = new ConcurrentHashMap<>();

  /** Creates a ValidateLibrary with all custom declarations and overloads. */
  ValidateLibrary() {}

  static Cel newCel() {
    ValidateLibrary validateLibrary = new ValidateLibrary();
    // NOTE: CelExtensions.strings() does not implement string.reverse() or strings.quote() which
    // are available in protovalidate-go. Fixed in https://github.com/google/cel-java/pull/998.
    return CelFactory.standardCelBuilder()
        .setOptions(CEL_OPTIONS)
        // Drop stdlib matches; CustomOverload provides a caching replacement.
        // Ref: https://github.com/google/cel-java/issues/1038
        .setStandardEnvironmentEnabled(false)
        .setStandardDeclarations(
            CelStandardDeclarations.newBuilder()
                .excludeFunctions(CelStandardDeclarations.StandardFunction.MATCHES)
                .build())
        .setStandardFunctions(
            CelStandardFunctions.newBuilder()
                .excludeFunctions(CelStandardFunctions.StandardFunction.MATCHES)
                .build())
        .addCompilerLibraries(validateLibrary, CelExtensions.strings())
        .addRuntimeLibraries(validateLibrary, CelExtensions.strings())
        .build();
  }

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
    runtimeBuilder.addFunctionBindings(CustomOverload.create(patternCache, CEL_OPTIONS));
  }
}
