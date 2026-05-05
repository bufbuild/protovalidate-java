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

import build.buf.protovalidate.exceptions.ValidationException;
import com.example.noimports.validationtest.Int64WrapperConst;
import com.example.noimports.validationtest.StringWrapperLen;
import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;
import org.junit.jupiter.api.Test;

/**
 * Targeted tests for {@link WrappedValueEvaluator}. Mirrors the wrapper-unwrap path that the native
 * dispatcher takes for {@code google.protobuf.*Value} fields. The conformance suite covers one
 * wrapper kind (DoubleValue) via parity tests; these cover the unwrap invariant directly.
 */
class WrappedValueEvaluatorTest {

  private final Validator nativeValidator =
      ValidatorFactory.newBuilder()
          .withConfig(Config.newBuilder().setEnableNativeRules().build())
          .build();

  @Test
  void absentWrapperFieldProducesNoViolation() throws ValidationException {
    // Field unset (proto3 message-typed field absent): there is no value to validate, so the
    // wrapped scalar evaluator should not be invoked. WrappedValueEvaluator's contract returns
    // NO_VIOLATIONS in that case.
    Int64WrapperConst msg = Int64WrapperConst.newBuilder().build();
    ValidationResult result = nativeValidator.validate(msg);
    assertThat(result.isSuccess()).isTrue();
  }

  @Test
  void presentWrapperWithDefaultInnerValueFiresRule() throws ValidationException {
    // Wrapper present with default inner (0). Rule is const=5 — 0 != 5 so the rule fires.
    // This proves the unwrap reaches the inner field rather than seeing the wrapper Message.
    Int64WrapperConst msg =
        Int64WrapperConst.newBuilder().setVal(Int64Value.newBuilder().build()).build();
    ValidationResult result = nativeValidator.validate(msg);
    assertThat(result.getViolations()).hasSize(1);
    build.buf.validate.Violation proto = result.getViolations().get(0).toProto();
    assertThat(proto.getRuleId()).isEqualTo("int64.const");
    assertThat(proto.getField().getElements(0).getFieldName()).isEqualTo("val");
  }

  @Test
  void presentWrapperWithViolatingValueProducesExpectedShape() throws ValidationException {
    StringWrapperLen msg =
        StringWrapperLen.newBuilder()
            .setVal(StringValue.newBuilder().setValue("ab").build())
            .build();
    ValidationResult result = nativeValidator.validate(msg);
    assertThat(result.getViolations()).hasSize(1);
    build.buf.validate.Violation proto = result.getViolations().get(0).toProto();
    assertThat(proto.getRuleId()).isEqualTo("string.min_len");
    // Field path points at the wrapper-typed field, not the synthetic inner "value".
    assertThat(proto.getField().getElements(0).getFieldName()).isEqualTo("val");
  }

  @Test
  void presentWrapperWithPassingValueProducesNoViolation() throws ValidationException {
    Int64WrapperConst msg =
        Int64WrapperConst.newBuilder().setVal(Int64Value.newBuilder().setValue(5).build()).build();
    assertThat(nativeValidator.validate(msg).isSuccess()).isTrue();
  }
}
