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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.projectnessie.cel.Ast;
import org.projectnessie.cel.Env;
import org.projectnessie.cel.Library;
import org.projectnessie.cel.Program;
import org.projectnessie.cel.common.types.Err;
import org.projectnessie.cel.common.types.ref.Val;
import org.projectnessie.cel.interpreter.Activation;

public class CustomOverloadTest {

  private final Env env = Env.newEnv(Library.Lib(new ValidateLibrary()));

  @Test
  public void testIsInf() {
    Map<String, Boolean> testCases =
        ImmutableMap.<String, Boolean>builder()
            .put("0.0.isInf()", false)
            .put("(1.0/0.0).isInf()", true)
            .put("(1.0/0.0).isInf(0)", true)
            .put("(1.0/0.0).isInf(1)", true)
            .put("(1.0/0.0).isInf(-1)", false)
            .put("(-1.0/0.0).isInf()", true)
            .put("(-1.0/0.0).isInf(0)", true)
            .put("(-1.0/0.0).isInf(1)", false)
            .put("(-1.0/0.0).isInf(-1)", true)
            .build();
    for (Map.Entry<String, Boolean> testCase : testCases.entrySet()) {
      Program.EvalResult result = eval(testCase.getKey());
      assertThat(result.getVal().booleanValue()).isEqualTo(testCase.getValue());
    }
  }

  @Test
  public void testIsInfUnsupported() {
    List<String> testCases = ImmutableList.of("'abc'.isInf()", "0.0.isInf('abc')");
    for (String testCase : testCases) {
      Val val = eval(testCase).getVal();
      assertThat(Err.isError(val)).isTrue();
      assertThatThrownBy(() -> val.convertToNative(Exception.class))
          .isInstanceOf(UnsupportedOperationException.class);
    }
  }

  @Test
  public void testIsNan() {
    Map<String, Boolean> testCases =
        ImmutableMap.<String, Boolean>builder()
            .put("0.0.isNan()", false)
            .put("(0.0/0.0).isNan()", true)
            .put("(1.0/0.0).isNan()", false)
            .build();
    for (Map.Entry<String, Boolean> testCase : testCases.entrySet()) {
      Program.EvalResult result = eval(testCase.getKey());
      assertThat(result.getVal().booleanValue()).isEqualTo(testCase.getValue());
    }
  }

  @Test
  public void testIsNanUnsupported() {
    List<String> testCases = ImmutableList.of("'foo'.isNan()");
    for (String testCase : testCases) {
      Val val = eval(testCase).getVal();
      assertThat(Err.isError(val)).isTrue();
      assertThatThrownBy(() -> val.convertToNative(Exception.class))
          .isInstanceOf(UnsupportedOperationException.class);
    }
  }

  @Test
  public void testUnique() {
    Map<String, Boolean> testCases =
        ImmutableMap.<String, Boolean>builder()
            .put("[].unique()", true)
            .put("[true].unique()", true)
            .put("[true, false].unique()", true)
            .put("[true, true].unique()", false)
            .put("[1, 2, 3].unique()", true)
            .put("[1, 2, 1].unique()", false)
            .put("[1u, 2u, 3u].unique()", true)
            .put("[1u, 2u, 2u].unique()", false)
            .put("[1.0, 2.0, 3.0].unique()", true)
            .put("[3.0,2.0,3.0].unique()", false)
            .put("['abc', 'def'].unique()", true)
            .put("['abc', 'abc'].unique()", false)
            .put("[b'abc', b'123'].unique()", true)
            .put("[b'123', b'123'].unique()", false)
            // Previously, the unique() method returned false here as both bytes were converted
            // to UTF-8. Since both contain invalid UTF-8, this would lead to them treated as equal
            // because they'd have the same substitution character.
            .put("[b'\\xFF', b'\\xFE'].unique()", true)
            .build();
    for (Map.Entry<String, Boolean> testCase : testCases.entrySet()) {
      Program.EvalResult result = eval(testCase.getKey());
      assertThat(result.getVal().booleanValue()).isEqualTo(testCase.getValue());
    }
  }

  @Test
  public void testUniqueUnsupported() {
    List<String> testCases = ImmutableList.of("1.unique()");
    for (String testCase : testCases) {
      Program.EvalResult result = eval(testCase);
      Val val = result.getVal();
      assertThat(Err.isError(val)).isTrue();
      assertThatThrownBy(() -> val.convertToNative(Exception.class))
          .isInstanceOf(UnsupportedOperationException.class);
    }
  }

  @Test
  public void testIsIpPrefix() {
    Map<String, Boolean> testCases =
        ImmutableMap.<String, Boolean>builder()
            .put("'1.2.3.0/24'.isIpPrefix()", true)
            .put("'1.2.3.4/24'.isIpPrefix()", true)
            .put("'1.2.3.0/24'.isIpPrefix(true)", true)
            .put("'1.2.3.4/24'.isIpPrefix(true)", false)
            .put("'fd7a:115c:a1e0:ab12:4843:cd96:626b:4000/118'.isIpPrefix()", true)
            .put("'fd7a:115c:a1e0:ab12:4843:cd96:626b:430b/118'.isIpPrefix()", true)
            .put("'fd7a:115c:a1e0:ab12:4843:cd96:626b:4000/118'.isIpPrefix(true)", true)
            .put("'fd7a:115c:a1e0:ab12:4843:cd96:626b:430b/118'.isIpPrefix(true)", false)
            .put("'1.2.3.4'.isIpPrefix()", false)
            .put("'fd7a:115c:a1e0:ab12:4843:cd96:626b:430b'.isIpPrefix()", false)
            .put("'1.2.3.0/24'.isIpPrefix(4)", true)
            .put("'1.2.3.4/24'.isIpPrefix(4)", true)
            .put("'1.2.3.0/24'.isIpPrefix(4,true)", true)
            .put("'1.2.3.4/24'.isIpPrefix(4,true)", false)
            .put("'fd7a:115c:a1e0:ab12:4843:cd96:626b:4000/118'.isIpPrefix(4)", false)
            .put("'fd7a:115c:a1e0:ab12:4843:cd96:626b:4000/118'.isIpPrefix(6)", true)
            .put("'fd7a:115c:a1e0:ab12:4843:cd96:626b:430b/118'.isIpPrefix(6)", true)
            .put("'fd7a:115c:a1e0:ab12:4843:cd96:626b:4000/118'.isIpPrefix(6,true)", true)
            .put("'fd7a:115c:a1e0:ab12:4843:cd96:626b:430b/118'.isIpPrefix(6,true)", false)
            .put("'1.2.3.0/24'.isIpPrefix(6)", false)
            .build();
    for (Map.Entry<String, Boolean> testCase : testCases.entrySet()) {
      Program.EvalResult result = eval(testCase.getKey());
      assertThat(result.getVal().booleanValue()).isEqualTo(testCase.getValue());
    }
  }

  @Test
  public void testIsIpPrefixUnsupported() {
    List<String> testCases =
        ImmutableList.of(
            "1.isIpPrefix()",
            "'1.2.3.0/24'.isIpPrefix('foo')",
            "'1.2.3.0/24'.isIpPrefix(4,'foo')",
            "'1.2.3.0/24'.isIpPrefix('foo',true)");
    for (String testCase : testCases) {
      Program.EvalResult result = eval(testCase);
      Val val = result.getVal();
      assertThat(Err.isError(val)).isTrue();
      assertThatThrownBy(() -> val.convertToNative(Exception.class))
          .isInstanceOf(UnsupportedOperationException.class);
    }
  }

  @Test
  public void testIsHostname() {
    Map<String, Boolean> testCases =
        ImmutableMap.<String, Boolean>builder()
            .put("'example.com'.isHostname()", true)
            .put("'example.123'.isHostname()", false)
            .build();
    for (Map.Entry<String, Boolean> testCase : testCases.entrySet()) {
      Program.EvalResult result = eval(testCase.getKey());
      assertThat(result.getVal().booleanValue())
          .as(
              "expected %s=%s, got=%s",
              testCase.getKey(), testCase.getValue(), !testCase.getValue())
          .isEqualTo(testCase.getValue());
    }
  }

  @Test
  public void testIsEmail() {
    Map<String, Boolean> testCases =
        ImmutableMap.<String, Boolean>builder()
            .put("'foo@example.com'.isEmail()", true)
            .put("'<foo@example.com>'.isEmail()", false)
            .put("'  foo@example.com'.isEmail()", false)
            .put("'foo@example.com    '.isEmail()", false)
            .build();
    for (Map.Entry<String, Boolean> testCase : testCases.entrySet()) {
      Program.EvalResult result = eval(testCase.getKey());
      assertThat(result.getVal().booleanValue())
          .as(
              "expected %s=%s, got=%s",
              testCase.getKey(), testCase.getValue(), !testCase.getValue())
          .isEqualTo(testCase.getValue());
    }
  }

  private Program.EvalResult eval(String source) {
    return eval(source, Activation.emptyActivation());
  }

  private Program.EvalResult eval(String source, Object vars) {
    Env.AstIssuesTuple parsed = env.parse(source);
    assertThat(parsed.hasIssues()).isFalse();
    Ast ast = parsed.getAst();
    return env.program(ast).eval(vars);
  }
}
