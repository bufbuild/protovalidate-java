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

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import build.buf.protovalidate.exceptions.ValidationException;
import com.example.noimports.validationtest.HttpHeaderName;
import com.example.noimports.validationtest.HttpHeaderNameLoose;
import com.example.noimports.validationtest.HttpHeaderValue;
import com.example.noimports.validationtest.HttpHeaderValueLoose;
import com.google.protobuf.Message;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for the {@code well_known_regex} oneof case in {@link StringRulesEvaluator}: HTTP header
 * name and value, in both strict and loose modes, plus the empty-header-name special case.
 */
class WellKnownRegexTest {

  private final Validator nativeValidator =
      ValidatorFactory.newBuilder()
          .withConfig(Config.newBuilder().setEnableNativeRules(true).build())
          .build();
  private final Validator celValidator =
      ValidatorFactory.newBuilder()
          .withConfig(Config.newBuilder().setEnableNativeRules(false).build())
          .build();

  @Test
  void headerName_strict_passesValidName() throws ValidationException {
    HttpHeaderName msg = HttpHeaderName.newBuilder().setVal("X-Request-Id").build();
    assertThat(nativeValidator.validate(msg).isSuccess()).isTrue();
  }

  @Test
  void headerName_strict_failsInvalidName() throws ValidationException {
    HttpHeaderName msg = HttpHeaderName.newBuilder().setVal("not a header").build();
    ValidationResult result = nativeValidator.validate(msg);
    assertThat(result.getViolations()).hasSize(1);
    build.buf.validate.Violation v = result.getViolations().get(0).toProto();
    assertThat(v.getRuleId()).isEqualTo("string.well_known_regex.header_name");
    assertThat(v.getMessage()).isEqualTo("must be a valid HTTP header name");
  }

  @Test
  void headerName_emptyValue_firesEmptyVariant() throws ValidationException {
    // Empty header name is a separate rule id with its own message.
    HttpHeaderName msg = HttpHeaderName.newBuilder().setVal("").build();
    ValidationResult result = nativeValidator.validate(msg);
    assertThat(result.getViolations()).hasSize(1);
    build.buf.validate.Violation v = result.getViolations().get(0).toProto();
    assertThat(v.getRuleId()).isEqualTo("string.well_known_regex.header_name_empty");
    assertThat(v.getMessage()).isEqualTo("value is empty, which is not a valid HTTP header name");
  }

  @Test
  void headerName_loose_acceptsValueStrictWouldReject() throws ValidationException {
    // Strict regex would reject spaces; loose just forbids null/CR/LF.
    HttpHeaderNameLoose msg =
        HttpHeaderNameLoose.newBuilder().setVal("any header with spaces").build();
    assertThat(nativeValidator.validate(msg).isSuccess()).isTrue();
  }

  @Test
  void headerValue_strict_passesValidValue() throws ValidationException {
    HttpHeaderValue msg = HttpHeaderValue.newBuilder().setVal("text/plain").build();
    assertThat(nativeValidator.validate(msg).isSuccess()).isTrue();
  }

  @Test
  void headerValue_strict_failsControlChar() throws ValidationException {
    // 0x01 is in the forbidden range for strict header values.
    HttpHeaderValue msg = HttpHeaderValue.newBuilder().setVal("").build();
    ValidationResult result = nativeValidator.validate(msg);
    assertThat(result.getViolations()).hasSize(1);
    build.buf.validate.Violation v = result.getViolations().get(0).toProto();
    assertThat(v.getRuleId()).isEqualTo("string.well_known_regex.header_value");
    assertThat(v.getMessage()).isEqualTo("must be a valid HTTP header value");
  }

  @Test
  void headerValue_emptyValueIsValid() throws ValidationException {
    // Header value pattern is '*' (zero-or-more), so empty is allowed under strict mode and
    // there is no header_value_empty variant.
    HttpHeaderValue msg = HttpHeaderValue.newBuilder().setVal("").build();
    assertThat(nativeValidator.validate(msg).isSuccess()).isTrue();
  }

  @Test
  void headerValue_loose_emptyValueIsValid() throws ValidationException {
    // The loose header value pattern is also '*', unlike the loose header name pattern.
    HttpHeaderValueLoose msg = HttpHeaderValueLoose.newBuilder().setVal("").build();
    assertThat(nativeValidator.validate(msg).isSuccess()).isTrue();
  }

  @Test
  void headerValue_loose_failsNullCrLf() throws ValidationException {
    for (String val : new String[] {"a\u0000b", "a\nb", "a\rb"}) {
      HttpHeaderValueLoose msg = HttpHeaderValueLoose.newBuilder().setVal(val).build();
      ValidationResult result = nativeValidator.validate(msg);
      assertThat(result.getViolations()).hasSize(1);
      build.buf.validate.Violation v = result.getViolations().get(0).toProto();
      assertThat(v.getRuleId()).isEqualTo("string.well_known_regex.header_value");
    }
  }

  // Comma in a strict header name is excluded: the bundled validate.proto still allows it via
  // the '+-.' range, while native follows upstream's fix (bufbuild/protovalidate#528).
  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "X-Request-Id",
        ":authority",
        "::method",
        "not a header",
        "text/plain; charset=utf-8",
        "tab\there",
        "a\u0000b",
        "a\u0001b",
        "a\u0008b",
        "a\nb",
        "a\rb",
        "a\u001fb",
        "a\u007fb",
        "a\u0080b",
        "naïve",
        "内容类型",
        "😀",
        "trailing\n",
        "\n",
      })
  void nativeMatchesCel(String val) throws ValidationException {
    assertSameRuleIds(HttpHeaderName.newBuilder().setVal(val).build());
    assertSameRuleIds(HttpHeaderNameLoose.newBuilder().setVal(val).build());
    assertSameRuleIds(HttpHeaderValue.newBuilder().setVal(val).build());
    assertSameRuleIds(HttpHeaderValueLoose.newBuilder().setVal(val).build());
  }

  private void assertSameRuleIds(Message msg) throws ValidationException {
    assertThat(ruleIds(nativeValidator.validate(msg)))
        .as("%s{val=%s}", msg.getDescriptorForType().getName(), msg)
        .isEqualTo(ruleIds(celValidator.validate(msg)));
  }

  private static List<String> ruleIds(ValidationResult result) {
    return result.getViolations().stream().map(v -> v.toProto().getRuleId()).collect(toList());
  }
}
