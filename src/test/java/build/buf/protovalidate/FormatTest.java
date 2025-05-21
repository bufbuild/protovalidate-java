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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cel.expr.conformance.test.SimpleTest;
import com.cel.expr.conformance.test.SimpleTestFile;
import com.cel.expr.conformance.test.SimpleTestSection;
import com.google.protobuf.Duration;
import com.google.protobuf.TextFormat;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.projectnessie.cel.Env;
import org.projectnessie.cel.EvalOption;
import org.projectnessie.cel.Library;
import org.projectnessie.cel.Program;
import org.projectnessie.cel.ProgramOption;
import org.projectnessie.cel.common.types.DoubleT;
import org.projectnessie.cel.common.types.Err.ErrException;
import org.projectnessie.cel.common.types.ListT;
import org.projectnessie.cel.common.types.StringT;
import org.projectnessie.cel.common.types.UintT;
import org.projectnessie.cel.common.types.pb.DefaultTypeAdapter;
import org.projectnessie.cel.common.types.ref.Val;
import org.projectnessie.cel.interpreter.Activation;

class FormatTest {
  // Version of the cel-spec that this implementation is conformant with
  // This should be kept in sync with the version in gradle.properties
  private static String CEL_SPEC_VERSION = "v0.24.0";

  private static Env env;

  private static SimpleTestSection formatSection;
  private static SimpleTestSection formatErrorSection;

  @BeforeAll
  static void setUp() {
    try {
      byte[] encoded =
          Files.readAllBytes(
              Paths.get(
                  "src/test/resources/testdata/string_ext_" + CEL_SPEC_VERSION + ".textproto"));
      String data = new String(encoded, StandardCharsets.UTF_8);
      SimpleTestFile.Builder bldr = SimpleTestFile.newBuilder();
      TextFormat.getParser().merge(data, bldr);
      SimpleTestFile testData = bldr.build();

      List<SimpleTestSection> fs =
          testData.getSectionList().stream()
              .filter(s -> s.getName().equals("format"))
              .collect(Collectors.toList());
      if (fs.size() == 1) {
        formatSection = fs.get(0);
      }

      List<SimpleTestSection> fes =
          testData.getSectionList().stream()
              .filter(s -> s.getName().equals("format_errors"))
              .collect(Collectors.toList());
      if (fes.size() == 1) {
        formatErrorSection = fes.get(0);
      }

      env = Env.newEnv(Library.Lib(new ValidateLibrary()));

    } catch (InvalidPathException ipe) {
      System.err.println(ipe);
    } catch (IOException ioe) {
      System.err.println(ioe);
    }
  }

  private static Stream<Arguments> getFormatTests() {
      List<Arguments> args = new ArrayList<Arguments>();
      for (SimpleTest test : formatSection.getTestList()) {
          args.add(Arguments.arguments(Named.named(test.getName(), test)));
      }

      return args.stream();
  }

  @ParameterizedTest()
  @MethodSource("getFormatTests")
  void testFormatSuccess(SimpleTest test) {
      Env.AstIssuesTuple ast = env.compile(test.getExpr());
      Map<String, String> map = new HashMap<String, String>();
      ProgramOption globals = ProgramOption.globals(map);
      List<ProgramOption> globs = new ArrayList<ProgramOption>();
      globs.add(globals);
      Program program =
          env.program(ast.getAst(), globals, ProgramOption.evalOptions(EvalOption.OptTrackState));
      program.eval(Activation.emptyActivation());
      // System.err.println(evalResult.getVal());
      // System.err.println(evalResult.getEvalDetails().getState());
  }

  @Test
  void testNotEnoughArgumentsThrows() {
    StringT one = StringT.stringOf("one");
    ListT val = (ListT) ListT.newValArrayList(null, new Val[] {one});

    assertThatThrownBy(
            () -> {
              Format.format("first value: %s and  %s", val);
            })
        .isInstanceOf(ErrException.class)
        .hasMessageContaining("format: not enough arguments");
  }

  @Test
  void testDouble() {
    ListT val =
        (ListT)
            ListT.newValArrayList(
                null,
                new Val[] {
                  DoubleT.doubleOf(-1.20000000000),
                  DoubleT.doubleOf(-1.2),
                  DoubleT.doubleOf(-1.230),
                  DoubleT.doubleOf(-1.002),
                  DoubleT.doubleOf(-0.1),
                  DoubleT.doubleOf(-.1),
                  DoubleT.doubleOf(-1),
                  DoubleT.doubleOf(-0.0),
                  DoubleT.doubleOf(0),
                  DoubleT.doubleOf(0.0),
                  DoubleT.doubleOf(1),
                  DoubleT.doubleOf(0.1),
                  DoubleT.doubleOf(.1),
                  DoubleT.doubleOf(1.002),
                  DoubleT.doubleOf(1.230),
                  DoubleT.doubleOf(1.20000000000)
                });
    String formatted =
        Format.format("%d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d", val);
    assertThat(formatted)
        .isEqualTo(
            "-1.2, -1.2, -1.23, -1.002, -0.1, -0.1, -1, -0, 0, 0, 1, 0.1, 0.1, 1.002, 1.23, 1.2");
  }

  @Test
  void testLargeDecimalValuesAreProperlyFormatted() {
    UintT largeDecimal = UintT.uintOf(999999999999L);
    ListT val = (ListT) ListT.newValArrayList(null, new Val[] {largeDecimal});
    String formatted = Format.format("%s", val);
    assertThat(formatted).isEqualTo("999999999999");
  }

  @Test
  void testDuration() {
    Duration duration = Duration.newBuilder().setSeconds(123).setNanos(45678).build();

    ListT val =
        (ListT) ListT.newGenericArrayList(DefaultTypeAdapter.Instance, new Duration[] {duration});
    String formatted = Format.format("%s", val);
    assertThat(formatted).isEqualTo("123.000045678s");
  }

  @Test
  void testEmptyDuration() {
    Duration duration = Duration.newBuilder().build();
    ListT val =
        (ListT) ListT.newGenericArrayList(DefaultTypeAdapter.Instance, new Duration[] {duration});
    String formatted = Format.format("%s", val);
    assertThat(formatted).isEqualTo("0s");
  }

  @Test
  void testDurationSecondsOnly() {
    Duration duration = Duration.newBuilder().setSeconds(123).build();

    ListT val =
        (ListT) ListT.newGenericArrayList(DefaultTypeAdapter.Instance, new Duration[] {duration});
    String formatted = Format.format("%s", val);
    assertThat(formatted).isEqualTo("123s");
  }

  @Test
  void testDurationNanosOnly() {
    Duration duration = Duration.newBuilder().setNanos(42).build();

    ListT val =
        (ListT) ListT.newGenericArrayList(DefaultTypeAdapter.Instance, new Duration[] {duration});
    String formatted = Format.format("%s", val);
    assertThat(formatted).isEqualTo("0.000000042s");
  }
}
