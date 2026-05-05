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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import build.buf.protovalidate.exceptions.ExecutionException;
import build.buf.protovalidate.exceptions.ValidationException;
import build.buf.validate.BytesRules;
import com.example.noimports.validationtest.ExampleBytesConst;
import com.example.noimports.validationtest.ExampleBytesIPv4;
import com.example.noimports.validationtest.ExampleBytesPattern;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.FieldDescriptor;
import org.junit.jupiter.api.Test;

/** Validator-level tests for {@link BytesRulesEvaluator}. */
class BytesRulesEvaluatorTest {

  private static Validator nativeValidator() {
    Config config = Config.newBuilder().setEnableNativeRules().build();
    return ValidatorFactory.newBuilder().withConfig(config).build();
  }

  @Test
  void bytesConstFailsAndCarriesExpectedShape() throws ValidationException {
    // const = "\x00\x99". Empty value (default) doesn't match.
    ExampleBytesConst msg = ExampleBytesConst.newBuilder().setVal(ByteString.EMPTY).build();
    ValidationResult result = nativeValidator().validate(msg);
    assertThat(result.getViolations()).hasSize(1);
    Violation v = result.getViolations().get(0);

    build.buf.validate.Violation proto = v.toProto();
    assertThat(proto.getRuleId()).isEqualTo("bytes.const");
    assertThat(proto.getMessage()).isEqualTo("must be 0099");
    assertThat(proto.getRule().getElements(0).getFieldName()).isEqualTo("bytes");
    assertThat(proto.getRule().getElements(1).getFieldName()).isEqualTo("const");

    // rule_value isn't in the proto — assert directly.
    Violation.FieldValue ruleValue = v.getRuleValue();
    assertThat(ruleValue).isNotNull();
    assertThat(ruleValue.getValue()).isEqualTo(ByteString.copyFrom(new byte[] {0x00, (byte) 0x99}));
    FieldDescriptor expectedDesc =
        BytesRules.getDescriptor().findFieldByNumber(BytesRules.CONST_FIELD_NUMBER);
    assertThat(ruleValue.getDescriptor()).isEqualTo(expectedDesc);
  }

  @Test
  void bytesPatternMatchesAlphanumericOnly() throws ValidationException {
    Validator v = nativeValidator();
    assertThat(
            v.validate(
                    ExampleBytesPattern.newBuilder().setVal(ByteString.copyFromUtf8("abc")).build())
                .isSuccess())
        .isTrue();
    assertThat(
            v.validate(
                    ExampleBytesPattern.newBuilder()
                        .setVal(ByteString.copyFromUtf8("abc1"))
                        .build())
                .isSuccess())
        .isFalse();
  }

  @Test
  void bytesPatternThrowsOnInvalidUtf8() {
    // Non-UTF-8 input + pattern rule → ExecutionException, matching Go's RuntimeError.
    ByteString invalidUtf8 = ByteString.copyFrom(new byte[] {(byte) 0xFF, (byte) 0xFE});
    ExampleBytesPattern msg = ExampleBytesPattern.newBuilder().setVal(invalidUtf8).build();
    Validator v = nativeValidator();
    assertThatThrownBy(() -> v.validate(msg))
        .isInstanceOf(ExecutionException.class)
        .hasMessageContaining("UTF-8");
  }

  @Test
  void bytesIpv4WellKnownAcceptsFourBytes() throws ValidationException {
    Validator v = nativeValidator();
    // 4 bytes — valid IPv4 size.
    assertThat(
            v.validate(
                    ExampleBytesIPv4.newBuilder().setVal(ByteString.copyFrom(new byte[4])).build())
                .isSuccess())
        .isTrue();
  }

  @Test
  void bytesIpv4WellKnownRejectsWrongSize() throws ValidationException {
    // 8 bytes — neither 0 nor 4, fails with the non-empty rule id.
    ExampleBytesIPv4 msg =
        ExampleBytesIPv4.newBuilder().setVal(ByteString.copyFrom(new byte[8])).build();
    ValidationResult result = nativeValidator().validate(msg);
    assertThat(result.getViolations()).hasSize(1);
    build.buf.validate.Violation proto = result.getViolations().get(0).toProto();
    assertThat(proto.getRuleId()).isEqualTo("bytes.ipv4");
    assertThat(proto.getMessage()).isEqualTo("must be a valid IPv4 address");
  }

  @Test
  void nativeAndCelProduceEqualViolationProto() throws ValidationException {
    ExampleBytesConst msg = ExampleBytesConst.newBuilder().setVal(ByteString.EMPTY).build();
    Validator nativeV = nativeValidator();
    Validator celV =
        ValidatorFactory.newBuilder()
            .withConfig(Config.newBuilder().setDisableNativeRules().build())
            .build();
    assertThat(nativeV.validate(msg).getViolations().get(0).toProto())
        .isEqualTo(celV.validate(msg).getViolations().get(0).toProto());
  }
}
