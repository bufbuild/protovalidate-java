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

package build.buf.protovalidate.rules;

import static org.assertj.core.api.Assertions.assertThat;

import build.buf.protovalidate.Config;
import build.buf.protovalidate.ValidationResult;
import build.buf.protovalidate.Validator;
import build.buf.protovalidate.ValidatorFactory;
import build.buf.protovalidate.exceptions.ValidationException;
import com.example.noimports.validationtest.HttpHeaderName;
import com.example.noimports.validationtest.HttpHeaderNameLoose;
import com.example.noimports.validationtest.HttpHeaderValue;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@code well_known_regex} oneof case in {@link StringRulesEvaluator}: HTTP header
 * name and value, in both strict and loose modes, plus the empty-header-name special case.
 */
class WellKnownRegexTest {

  private final Validator nativeValidator =
      ValidatorFactory.newBuilder()
          .withConfig(Config.newBuilder().setEnableNativeRules().build())
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
}
