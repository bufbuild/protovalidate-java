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
import build.buf.protovalidate.Violation;
import build.buf.protovalidate.exceptions.ValidationException;
import build.buf.validate.StringRules;
import com.example.noimports.validationtest.ExampleStringConst;
import com.example.noimports.validationtest.ExampleStringEmail;
import com.example.noimports.validationtest.ExampleStringHostAndPort;
import com.example.noimports.validationtest.ExampleStringMinMaxLen;
import com.google.protobuf.Descriptors.FieldDescriptor;
import org.junit.jupiter.api.Test;

/** Validator-level tests for {@link StringRulesEvaluator}. */
class StringRulesEvaluatorTest {

  private static Validator nativeValidator() {
    Config config = Config.newBuilder().setEnableNativeRules().build();
    return ValidatorFactory.newBuilder().withConfig(config).build();
  }

  @Test
  void stringConstFailsAndCarriesExpectedShape() throws ValidationException {
    ExampleStringConst msg = ExampleStringConst.newBuilder().setVal("nope").build();
    ValidationResult result = nativeValidator().validate(msg);
    assertThat(result.getViolations()).hasSize(1);
    Violation v = result.getViolations().get(0);

    build.buf.validate.Violation proto = v.toProto();
    assertThat(proto.getRuleId()).isEqualTo("string.const");
    assertThat(proto.getMessage()).isEqualTo("must equal `abcd`");

    Violation.FieldValue ruleValue = v.getRuleValue();
    assertThat(ruleValue).isNotNull();
    assertThat(ruleValue.getValue()).isEqualTo("abcd");
    FieldDescriptor expectedDesc =
        StringRules.getDescriptor().findFieldByNumber(StringRules.CONST_FIELD_NUMBER);
    assertThat(ruleValue.getDescriptor()).isEqualTo(expectedDesc);
  }

  @Test
  void emailWellKnownAcceptsValidAndRejectsInvalid() throws ValidationException {
    Validator v = nativeValidator();
    assertThat(
            v.validate(ExampleStringEmail.newBuilder().setVal("alice@example.com").build())
                .isSuccess())
        .isTrue();
    assertThat(
            v.validate(ExampleStringEmail.newBuilder().setVal("not-an-email").build()).isSuccess())
        .isFalse();
  }

  @Test
  void emailWellKnownReportsEmptyVariant() throws ValidationException {
    // Empty string fires the *_empty variant rule id.
    ExampleStringEmail msg = ExampleStringEmail.newBuilder().setVal("").build();
    ValidationResult result = nativeValidator().validate(msg);
    assertThat(result.getViolations()).hasSize(1);
    build.buf.validate.Violation proto = result.getViolations().get(0).toProto();
    assertThat(proto.getRuleId()).isEqualTo("string.email_empty");
    assertThat(proto.getMessage()).contains("value is empty");
  }

  @Test
  void minMaxLenAppliesCharacterCounts() throws ValidationException {
    Validator v = nativeValidator();
    // min_len=2, max_len=5
    assertThat(v.validate(ExampleStringMinMaxLen.newBuilder().setVal("ab").build()).isSuccess())
        .isTrue();
    assertThat(v.validate(ExampleStringMinMaxLen.newBuilder().setVal("abcde").build()).isSuccess())
        .isTrue();
    // 1 character — too short.
    ValidationResult tooShort = v.validate(ExampleStringMinMaxLen.newBuilder().setVal("a").build());
    assertThat(tooShort.getViolations()).hasSize(1);
    assertThat(tooShort.getViolations().get(0).toProto().getRuleId()).isEqualTo("string.min_len");
    // 6 characters — too long.
    ValidationResult tooLong =
        v.validate(ExampleStringMinMaxLen.newBuilder().setVal("abcdef").build());
    assertThat(tooLong.getViolations()).hasSize(1);
    assertThat(tooLong.getViolations().get(0).toProto().getRuleId()).isEqualTo("string.max_len");
  }

  @Test
  void minLenCountsCodePointsNotJavaChars() throws ValidationException {
    // Each emoji is a single code point but two Java chars (surrogate pair). min_len=2 should
    // count code points, not chars.
    Validator v = nativeValidator();
    assertThat(v.validate(ExampleStringMinMaxLen.newBuilder().setVal("😀😀").build()).isSuccess())
        .isTrue();
    // One emoji = 1 code point, fails min_len=2.
    assertThat(v.validate(ExampleStringMinMaxLen.newBuilder().setVal("😀").build()).isSuccess())
        .isFalse();
  }

  @Test
  void hostAndPortAcceptsValidAndRejectsInvalid() throws ValidationException {
    Validator v = nativeValidator();
    assertThat(
            v.validate(ExampleStringHostAndPort.newBuilder().setVal("example.com:8080").build())
                .isSuccess())
        .isTrue();
    assertThat(
            v.validate(ExampleStringHostAndPort.newBuilder().setVal("not-a-host-and-port").build())
                .isSuccess())
        .isFalse();
  }

  @Test
  void nativeAndCelProduceEqualViolationProto() throws ValidationException {
    ExampleStringConst msg = ExampleStringConst.newBuilder().setVal("nope").build();
    Validator nativeV = nativeValidator();
    Validator celV =
        ValidatorFactory.newBuilder()
            .withConfig(Config.newBuilder().setDisableNativeRules().build())
            .build();
    assertThat(nativeV.validate(msg).getViolations().get(0).toProto())
        .isEqualTo(celV.validate(msg).getViolations().get(0).toProto());
  }
}
