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

import static org.assertj.core.api.Assertions.*;

import cel.expr.conformance.proto3.TestAllTypes;
import com.cel.expr.Decl;
import com.cel.expr.ExprValue;
import com.cel.expr.Value;
import com.cel.expr.conformance.test.SimpleTest;
import com.cel.expr.conformance.test.SimpleTestFile;
import com.cel.expr.conformance.test.SimpleTestSection;
import com.google.protobuf.TextFormat;
import dev.cel.bundle.Cel;
import dev.cel.bundle.CelBuilder;
import dev.cel.bundle.CelFactory;
import dev.cel.common.CelOptions;
import dev.cel.common.CelValidationException;
import dev.cel.common.CelValidationResult;
import dev.cel.common.types.SimpleType;
import dev.cel.runtime.CelEvaluationException;
import dev.cel.runtime.CelRuntime.Program;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class FormatTest {
  // Version of the cel-spec that this implementation is conformant with
  // This should be kept in sync with the version in gradle.properties
  private static final String CEL_SPEC_VERSION = "v0.24.0";

  private static Cel cel;

  private static List<SimpleTest> formatTests;
  private static List<SimpleTest> formatErrorTests;

  @BeforeAll
  public static void setUp() throws Exception {
    // The test data from the cel-spec conformance tests
    List<SimpleTestSection> celSpecSections =
        loadTestData("src/test/resources/testdata/string_ext_" + CEL_SPEC_VERSION + ".textproto");
    // Our supplemental tests of functionality not in the cel conformance file, but defined in the
    // spec.
    List<SimpleTestSection> supplementalSections =
        loadTestData("src/test/resources/testdata/string_ext_supplemental.textproto");

    // Combine the test data from both files into one
    List<SimpleTestSection> sections =
        Stream.concat(celSpecSections.stream(), supplementalSections.stream())
            .collect(Collectors.toList());

    // Find the format tests which test successful formatting
    formatTests =
        sections.stream()
            .filter(s -> s.getName().equals("format"))
            .flatMap(s -> s.getTestList().stream())
            .collect(Collectors.toList());

    // Find the format error tests which test errors during formatting
    formatErrorTests =
        sections.stream()
            .filter(s -> s.getName().equals("format_errors"))
            .flatMap(s -> s.getTestList().stream())
            .collect(Collectors.toList());

    ValidateLibrary validateLibrary = new ValidateLibrary();
    cel =
        CelFactory.standardCelBuilder()
            .addCompilerLibraries(validateLibrary)
            .addRuntimeLibraries(validateLibrary)
            .setOptions(
                CelOptions.DEFAULT.toBuilder().evaluateCanonicalTypesToNativeValues(true).build())
            .build();
  }

  @ParameterizedTest
  @MethodSource("getFormatTests")
  void testFormatSuccess(SimpleTest test) throws CelValidationException, CelEvaluationException {
    Object result = evaluate(test);
    assertThat(result).isEqualTo(getExpectedResult(test));
    assertThat(result).isInstanceOf(String.class);
  }

  @ParameterizedTest
  @MethodSource("getFormatErrorTests")
  void testFormatError(SimpleTest test) {
    assertThatThrownBy(() -> evaluate(test)).isInstanceOf(CelEvaluationException.class);
  }

  // Loads test data from the given text format file
  private static List<SimpleTestSection> loadTestData(String fileName) throws Exception {
    byte[] encoded = Files.readAllBytes(Paths.get(fileName));
    String data = new String(encoded, StandardCharsets.UTF_8);
    SimpleTestFile.Builder bldr = SimpleTestFile.newBuilder();
    TextFormat.getParser().merge(data, bldr);
    SimpleTestFile testData = bldr.build();

    return testData.getSectionList();
  }

  // Runs a test by extending the cel environment with the specified
  // types, variables and declarations, then evaluating it with the cel runtime.
  private static Object evaluate(SimpleTest test)
      throws CelValidationException, CelEvaluationException {

    CelBuilder builder = cel.toCelBuilder().addMessageTypes(TestAllTypes.getDescriptor());
    addDecls(builder, test);
    Cel newCel = builder.build();

    CelValidationResult validationResult = newCel.compile(test.getExpr());
    if (!validationResult.getAllIssues().isEmpty()) {
      fail("error building AST for evaluation: " + validationResult.getIssueString());
    }
    Program program = newCel.createProgram(validationResult.getAst());
    return program.eval(buildVariables(test.getBindingsMap()));
  }

  private static Stream<Arguments> getTestStream(List<SimpleTest> tests) {
    List<Arguments> args = new ArrayList<>();
    for (SimpleTest test : tests) {
      args.add(Arguments.arguments(Named.named(test.getName(), test)));
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
    Map<String, Object> vars = new HashMap<>();
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
  private static void addDecls(CelBuilder builder, SimpleTest test) {
    for (Decl decl : test.getTypeEnvList()) {
      if (decl.hasIdent()) {
        Decl.IdentDecl ident = decl.getIdent();
        com.cel.expr.Type type = ident.getType();
        if (type.hasPrimitive()) {
          if (type.getPrimitive() == com.cel.expr.Type.PrimitiveType.STRING) {
            builder.addVar(decl.getName(), SimpleType.STRING);
          }
        }
      }
    }
  }
}
