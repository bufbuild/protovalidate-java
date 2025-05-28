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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import cel.expr.conformance.proto3.TestAllTypes;
import com.cel.expr.Decl;
import com.cel.expr.ExprValue;
import com.cel.expr.Value;
import com.cel.expr.conformance.test.SimpleTest;
import com.cel.expr.conformance.test.SimpleTestFile;
import com.cel.expr.conformance.test.SimpleTestSection;
import com.google.protobuf.TextFormat;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.projectnessie.cel.Env;
import org.projectnessie.cel.EnvOption;
import org.projectnessie.cel.EvalOption;
import org.projectnessie.cel.Library;
import org.projectnessie.cel.Program;
import org.projectnessie.cel.ProgramOption;
import org.projectnessie.cel.checker.Decls;
import org.projectnessie.cel.common.types.ref.TypeEnum;
import org.projectnessie.cel.interpreter.Activation;

class FormatTest {
  // Version of the cel-spec that this implementation is conformant with
  // This should be kept in sync with the version in gradle.properties
  private static String CEL_SPEC_VERSION = "v0.24.0";

  private static Env env;

  private static List<SimpleTest> formatTests;
  private static List<SimpleTest> formatErrorTests;

  private static List<String> SKIPPED_TESTS =
      Arrays.asList(
          // Success Tests
          // found no matching overload for 'format' applied to 'string.(list(type(string)))'
          "type() support for string",
          // found no matching overload for 'format' applied to 'string.(list(map(string, dyn)))'
          "map support for string",
          // found no matching overload for 'format' applied to 'string.(list(map(dyn, dyn)))'
          "map support (all key types)",
          // found no matching overload for 'format' applied to 'string.(list(map(dyn, dyn)))'
          "dyntype support for maps",
          // Error Tests
          // found no matching overload for 'format' applied to
          // 'string.(list(cel.expr.conformance.proto3.TestAllTypes))'
          "object not allowed",
          // found no matching overload for 'format' applied to 'string.(list(map(int, dyn)))'
          "object inside map");

  @BeforeAll
  static void setUp() throws Exception {
    byte[] encoded =
        Files.readAllBytes(
            Paths.get("src/test/resources/testdata/string_ext_" + CEL_SPEC_VERSION + ".textproto"));
    String data = new String(encoded, StandardCharsets.UTF_8);
    SimpleTestFile.Builder bldr = SimpleTestFile.newBuilder();
    TextFormat.getParser().merge(data, bldr);
    SimpleTestFile testData = bldr.build();

    List<SimpleTestSection> sections = testData.getSectionList();

    // Find the format tests which test successful formatting
    // Defaults to an empty list if nothing is found
    formatTests =
        sections.stream()
            .filter(s -> s.getName().equals("format"))
            .findFirst()
            .map(SimpleTestSection::getTestList)
            .orElse(Collections.emptyList());

    // Find the format error tests which test errors during formatting
    // Defaults to an empty list if nothing is found
    formatErrorTests =
        sections.stream()
            .filter(s -> s.getName().equals("format_errors"))
            .findFirst()
            .map(SimpleTestSection::getTestList)
            .orElse(Collections.emptyList());

    env = Env.newEnv(Library.Lib(new ValidateLibrary()));
  }

  @ParameterizedTest()
  @MethodSource("getFormatTests")
  void testFormatSuccess(SimpleTest test) {
    Program.EvalResult result = evaluate(test);
    assertThat(result.getVal().value()).isEqualTo(getExpectedResult(test));
    assertThat(result.getVal().type().typeEnum()).isEqualTo(TypeEnum.String);
  }

  @ParameterizedTest()
  @MethodSource("getFormatErrorTests")
  void testFormatError(SimpleTest test) {
    Program.EvalResult result = evaluate(test);
    assertThat(result.getVal().value()).isEqualTo(getExpectedResult(test));
    assertThat(result.getVal().type().typeEnum()).isEqualTo(TypeEnum.Err);
  }

  // Runs a test by extending the cel environment with the specified
  // types, variables and declarations, then evaluating it with the cel runtime.
  private static Program.EvalResult evaluate(SimpleTest test) {
    List<com.google.api.expr.v1alpha1.Decl> decls = buildDecls(test);

    TestAllTypes msg = TestAllTypes.newBuilder().getDefaultInstanceForType();
    Env newEnv = env.extend(EnvOption.types(msg), EnvOption.declarations(decls));

    Env.AstIssuesTuple ast = newEnv.compile(test.getExpr());
    if (ast.hasIssues()) {
      fail("error building AST for evaluation: " + ast.getIssues().toString());
    }
    Map<String, Object> vars = buildVariables(test.getBindingsMap());
    ProgramOption globals = ProgramOption.globals(vars);
    Program program =
        newEnv.program(ast.getAst(), globals, ProgramOption.evalOptions(EvalOption.OptTrackState));

    return program.eval(Activation.emptyActivation());
  }

  private static Stream<Arguments> getTestStream(List<SimpleTest> tests) {
    List<Arguments> args = new ArrayList<Arguments>();
    for (SimpleTest test : tests) {
      if (!SKIPPED_TESTS.contains(test.getName())) {
        args.add(Arguments.arguments(Named.named(test.getName(), test)));
      }
    }

    return args.stream();
  }

  private static Stream<Arguments> getFormatTests() {
    return getTestStream(formatTests);
  }

  private static Stream<Arguments> getFormatErrorTests() {
    return getTestStream(formatErrorTests);
  }

  // Builds the variable definitions to be used during evaluation
  private static Map<String, Object> buildVariables(Map<String, ExprValue> bindings) {
    Map<String, Object> vars = new HashMap<String, Object>();
    for (Map.Entry<String, ExprValue> entry : bindings.entrySet()) {
      ExprValue exprValue = entry.getValue();
      if (exprValue.hasValue()) {
        Value val = exprValue.getValue();
        if (val.hasStringValue()) {
          vars.put(entry.getKey(), val.getStringValue());
        }
      }
    }
    return vars;
  }

  // Gets the expected result for a given test
  private static String getExpectedResult(SimpleTest test) {
    if (test.hasValue()) {
      if (test.getValue().hasStringValue()) {
        return test.getValue().getStringValue();
      }
    } else if (test.hasEvalError()) {
      // Note that we only expect a single eval error for all the conformance tests
      if (test.getEvalError().getErrorsList().size() == 1) {
        return test.getEvalError().getErrorsList().get(0).getMessage();
      }
    }
    return "";
  }

  // Builds the declarations for a given test
  private static List<com.google.api.expr.v1alpha1.Decl> buildDecls(SimpleTest test) {
    List<com.google.api.expr.v1alpha1.Decl> decls =
        new ArrayList<com.google.api.expr.v1alpha1.Decl>();
    for (Decl decl : test.getTypeEnvList()) {
      if (decl.hasIdent()) {
        Decl.IdentDecl ident = decl.getIdent();
        com.cel.expr.Type type = ident.getType();
        if (type.hasPrimitive()) {
          if (type.getPrimitive() == com.cel.expr.Type.PrimitiveType.STRING) {
            decls.add(Decls.newVar(decl.getName(), Decls.String));
          }
        }
      }
    }
    return decls;
  }
}
