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

import java.util.Arrays;
import java.util.Collections;
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
    assertThat(evalToBool("0.0.isInf()")).isFalse();
    assertThat(evalToBool("(1.0/0.0).isInf()")).isTrue();
    assertThat(evalToBool("(1.0/0.0).isInf(0)")).isTrue();
    assertThat(evalToBool("(1.0/0.0).isInf(1)")).isTrue();
    assertThat(evalToBool("(1.0/0.0).isInf(-1)")).isFalse();
    assertThat(evalToBool("(-1.0/0.0).isInf()")).isTrue();
    assertThat(evalToBool("(-1.0/0.0).isInf(0)")).isTrue();
    assertThat(evalToBool("(-1.0/0.0).isInf(1)")).isFalse();
    assertThat(evalToBool("(-1.0/0.0).isInf(-1)")).isTrue();
  }

  @Test
  public void testIsInfUnsupported() {
    for (String testCase : Arrays.asList("'abc'.isInf()", "0.0.isInf('abc')")) {
      Val val = eval(testCase).getVal();
      assertThat(Err.isError(val)).isTrue();
      assertThatThrownBy(() -> val.convertToNative(Exception.class))
          .isInstanceOf(UnsupportedOperationException.class);
    }
  }

  @Test
  public void testIsNan() {
    assertThat(evalToBool("0.0.isNan()")).isFalse();
    assertThat(evalToBool("(0.0/0.0).isNan()")).isTrue();
    assertThat(evalToBool("(1.0/0.0).isNan()")).isFalse();
  }

  @Test
  public void testIsNanUnsupported() {
    for (String testCase : Collections.singletonList("'foo'.isNan()")) {
      Val val = eval(testCase).getVal();
      assertThat(Err.isError(val)).isTrue();
      assertThatThrownBy(() -> val.convertToNative(Exception.class))
          .isInstanceOf(UnsupportedOperationException.class);
    }
  }

  @Test
  public void testUnique() {
    assertThat(evalToBool("[].unique()")).isTrue();
    assertThat(evalToBool("[true].unique()")).isTrue();
    assertThat(evalToBool("[true, false].unique()")).isTrue();
    assertThat(evalToBool("[true, true].unique()")).isFalse();
    assertThat(evalToBool("[1, 2, 3].unique()")).isTrue();
    assertThat(evalToBool("[1, 2, 1].unique()")).isFalse();
    assertThat(evalToBool("[1u, 2u, 3u].unique()")).isTrue();
    assertThat(evalToBool("[1u, 2u, 2u].unique()")).isFalse();
    assertThat(evalToBool("[1.0, 2.0, 3.0].unique()")).isTrue();
    assertThat(evalToBool("[3.0,2.0,3.0].unique()")).isFalse();
    assertThat(evalToBool("['abc', 'def'].unique()")).isTrue();
    assertThat(evalToBool("['abc', 'abc'].unique()")).isFalse();
    assertThat(evalToBool("[b'abc', b'123'].unique()")).isTrue();
    assertThat(evalToBool("[b'123', b'123'].unique()")).isFalse();
    // Previously, the unique() method returned false here as both bytes were converted
    // to UTF-8. Since both contain invalid UTF-8, this would lead to them treated as equal
    // because they'd have the same substitution character.
    assertThat(evalToBool("[b'\\xFF', b'\\xFE'].unique()")).isTrue();
  }

  @Test
  public void testUniqueUnsupported() {
    for (String testCase : Collections.singletonList("1.unique()")) {
      Program.EvalResult result = eval(testCase);
      Val val = result.getVal();
      assertThat(Err.isError(val)).isTrue();
      assertThatThrownBy(() -> val.convertToNative(Exception.class))
          .isInstanceOf(UnsupportedOperationException.class);
    }
  }

  @Test
  public void testIsIpPrefix() {
    assertThat(evalToBool("'1.2.3.0/24'.isIpPrefix()")).isTrue();
    assertThat(evalToBool("'1.2.3.4/24'.isIpPrefix()")).isTrue();
    assertThat(evalToBool("'1.2.3.0/24'.isIpPrefix(true)")).isTrue();
    assertThat(evalToBool("'1.2.3.4/24'.isIpPrefix(true)")).isFalse();
    assertThat(evalToBool("'fd7a:115c:a1e0:ab12:4843:cd96:626b:4000/118'.isIpPrefix()")).isTrue();
    assertThat(evalToBool("'fd7a:115c:a1e0:ab12:4843:cd96:626b:430b/118'.isIpPrefix()")).isTrue();
    assertThat(evalToBool("'fd7a:115c:a1e0:ab12:4843:cd96:626b:4000/118'.isIpPrefix(true)"))
        .isTrue();
    assertThat(evalToBool("'fd7a:115c:a1e0:ab12:4843:cd96:626b:430b/118'.isIpPrefix(true)"))
        .isFalse();
    assertThat(evalToBool("'1.2.3.4'.isIpPrefix()")).isFalse();
    assertThat(evalToBool("'fd7a:115c:a1e0:ab12:4843:cd96:626b:430b'.isIpPrefix()")).isFalse();
    assertThat(evalToBool("'1.2.3.0/24'.isIpPrefix(4)")).isTrue();
    assertThat(evalToBool("'1.2.3.4/24'.isIpPrefix(4)")).isTrue();
    assertThat(evalToBool("'1.2.3.0/24'.isIpPrefix(4,true)")).isTrue();
    assertThat(evalToBool("'1.2.3.4/24'.isIpPrefix(4,true)")).isFalse();
    assertThat(evalToBool("'fd7a:115c:a1e0:ab12:4843:cd96:626b:4000/118'.isIpPrefix(4)")).isFalse();
    assertThat(evalToBool("'fd7a:115c:a1e0:ab12:4843:cd96:626b:4000/118'.isIpPrefix(6)")).isTrue();
    assertThat(evalToBool("'fd7a:115c:a1e0:ab12:4843:cd96:626b:430b/118'.isIpPrefix(6)")).isTrue();
    assertThat(evalToBool("'fd7a:115c:a1e0:ab12:4843:cd96:626b:4000/118'.isIpPrefix(6,true)"))
        .isTrue();
    assertThat(evalToBool("'fd7a:115c:a1e0:ab12:4843:cd96:626b:430b/118'.isIpPrefix(6,true)"))
        .isFalse();
    assertThat(evalToBool("'1.2.3.0/24'.isIpPrefix(6)")).isFalse();
  }

  @Test
  public void testIsIpPrefixUnsupported() {
    for (String testCase :
        Arrays.asList(
            "1.isIpPrefix()",
            "'1.2.3.0/24'.isIpPrefix('foo')",
            "'1.2.3.0/24'.isIpPrefix(4,'foo')",
            "'1.2.3.0/24'.isIpPrefix('foo',true)")) {
      Program.EvalResult result = eval(testCase);
      Val val = result.getVal();
      assertThat(Err.isError(val)).isTrue();
      assertThatThrownBy(() -> val.convertToNative(Exception.class))
          .isInstanceOf(UnsupportedOperationException.class);
    }
  }

  @Test
  public void testIsHostname() {
    assertThat(evalToBool("'example.com'.isHostname()")).isTrue();
    assertThat(evalToBool("'example.123'.isHostname()")).isFalse();
  }

  @Test
  public void testIsEmail() {
    assertThat(evalToBool("'foo@example.com'.isEmail()")).isTrue();
    assertThat(evalToBool("'<foo@example.com>'.isEmail()")).isFalse();
    assertThat(evalToBool("'  foo@example.com'.isEmail()")).isFalse();
    assertThat(evalToBool("'foo@example.com    '.isEmail()")).isFalse();
  }

  @Test
  public void testBytesContains() {
    assertThat(evalToBool("bytes('12345').contains(bytes(''))")).isTrue();
    assertThat(evalToBool("bytes('12345').contains(bytes('1'))")).isTrue();
    assertThat(evalToBool("bytes('12345').contains(bytes('5'))")).isTrue();
    assertThat(evalToBool("bytes('12345').contains(bytes('123'))")).isTrue();
    assertThat(evalToBool("bytes('12345').contains(bytes('234'))")).isTrue();
    assertThat(evalToBool("bytes('12345').contains(bytes('345'))")).isTrue();
    assertThat(evalToBool("bytes('12345').contains(bytes('12345'))")).isTrue();

    assertThat(evalToBool("bytes('12345').contains(bytes('6'))")).isFalse();
    assertThat(evalToBool("bytes('12345').contains(bytes('13'))")).isFalse();
    assertThat(evalToBool("bytes('12345').contains(bytes('35'))")).isFalse();
    assertThat(evalToBool("bytes('12345').contains(bytes('123456'))")).isFalse();
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

  private boolean evalToBool(String source) {
    Program.EvalResult result = eval(source);
    return result.getVal().booleanValue();
  }
}
