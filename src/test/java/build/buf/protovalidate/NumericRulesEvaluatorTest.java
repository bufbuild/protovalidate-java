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
import build.buf.validate.DoubleRules;
import build.buf.validate.Int32Rules;
import build.buf.validate.UInt32Rules;
import com.example.noimports.validationtest.ExampleDoubleIn;
import com.example.noimports.validationtest.ExampleFloatFinite;
import com.example.noimports.validationtest.ExampleInt32Const;
import com.example.noimports.validationtest.ExampleInt32GtLt;
import com.example.noimports.validationtest.ExampleUint32Gt;
import com.google.protobuf.Descriptors.FieldDescriptor;
import org.junit.jupiter.api.Test;

/**
 * Validator-level tests for {@link NumericRulesEvaluator}. Per-kind comparison correctness is
 * already covered comprehensively by the conformance suite (44 cases × 12 kinds); these tests focus
 * on the things conformance can't catch:
 *
 * <ul>
 *   <li>{@link Violation#getRuleValue()} shape — not part of the {@code Violation} proto so the
 *       conformance harness can't assert on it.
 *   <li>Unsigned comparison correctness with values above {@code Integer.MAX_VALUE} as a targeted
 *       regression test for {@code Integer.compareUnsigned} wiring.
 *   <li>{@code finite}-rule {@code NaN}/{@code Inf} dispatch.
 * </ul>
 */
class NumericRulesEvaluatorTest {

  private static Validator nativeValidator() {
    Config config = Config.newBuilder().setEnableNativeRules(true).build();
    return ValidatorFactory.newBuilder().withConfig(config).build();
  }

  @Test
  void int32ConstFailsAndCarriesExpectedShape() throws ValidationException {
    // Default 0 != const 5.
    ExampleInt32Const msg = ExampleInt32Const.newBuilder().setVal(0).build();
    ValidationResult result = nativeValidator().validate(msg);
    assertThat(result.getViolations()).hasSize(1);

    Violation v = result.getViolations().get(0);
    build.buf.validate.Violation proto = v.toProto();
    assertThat(proto.getRuleId()).isEqualTo("int32.const");
    assertThat(proto.getMessage()).isEqualTo("must equal 5");
    assertThat(proto.getField().getElements(0).getFieldName()).isEqualTo("val");
    assertThat(proto.getRule().getElements(0).getFieldName()).isEqualTo("int32");
    assertThat(proto.getRule().getElements(1).getFieldName()).isEqualTo("const");

    // Action item from Phase 1 / Task #3: rule_value isn't in the Violation proto, so the
    // conformance suite can't catch divergence on it. Assert directly here.
    Violation.FieldValue ruleValue = v.getRuleValue();
    assertThat(ruleValue).isNotNull();
    assertThat(ruleValue.getValue()).isEqualTo(5);
    FieldDescriptor expectedDesc =
        Int32Rules.getDescriptor().findFieldByNumber(Int32Rules.CONST_FIELD_NUMBER);
    assertThat(ruleValue.getDescriptor()).isEqualTo(expectedDesc);
  }

  @Test
  void int32GtLtRangeProducesCombinedRuleId() throws ValidationException {
    // Default 0 violates gt=0 (lower bound). Combined gt+lt produces "int32.gt_lt" rule id with
    // a single combined message.
    ExampleInt32GtLt msg = ExampleInt32GtLt.newBuilder().setVal(0).build();
    ValidationResult result = nativeValidator().validate(msg);
    assertThat(result.getViolations()).hasSize(1);
    build.buf.validate.Violation proto = result.getViolations().get(0).toProto();
    assertThat(proto.getRuleId()).isEqualTo("int32.gt_lt");
    assertThat(proto.getMessage()).isEqualTo("must be greater than 0 and less than 10");
  }

  @Test
  void uint32UnsignedComparisonHandlesValuesAboveSignedMax() throws ValidationException {
    // Rule: gt = 2147483648 (which is Integer.MAX_VALUE + 1 as unsigned). A naive signed compare
    // would interpret this threshold as -2147483648 and accept any positive int. With unsigned
    // semantics, 1 must NOT satisfy gt=2147483648.
    ExampleUint32Gt msg = ExampleUint32Gt.newBuilder().setVal(1).build();
    ValidationResult result = nativeValidator().validate(msg);
    assertThat(result.getViolations()).hasSize(1);
    build.buf.validate.Violation proto = result.getViolations().get(0).toProto();
    assertThat(proto.getRuleId()).isEqualTo("uint32.gt");
    assertThat(proto.getMessage()).isEqualTo("must be greater than 2147483648");

    Violation.FieldValue ruleValue = result.getViolations().get(0).getRuleValue();
    assertThat(ruleValue).isNotNull();
    // Stored as Java's signed Integer with the bit pattern of unsigned 2147483648.
    assertThat(ruleValue.getValue()).isEqualTo(Integer.MIN_VALUE);
    FieldDescriptor expectedDesc =
        UInt32Rules.getDescriptor().findFieldByNumber(UInt32Rules.GT_FIELD_NUMBER);
    assertThat(ruleValue.getDescriptor()).isEqualTo(expectedDesc);
  }

  @Test
  void uint32UnsignedComparisonAcceptsValueAboveThreshold() throws ValidationException {
    // 3000000000 (unsigned) must satisfy gt=2147483648 (unsigned).
    ExampleUint32Gt msg = ExampleUint32Gt.newBuilder().setVal((int) 3_000_000_000L).build();
    ValidationResult result = nativeValidator().validate(msg);
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getViolations()).isEmpty();
  }

  @Test
  void floatFiniteFailsForNaNAndInf() throws ValidationException {
    Validator v = nativeValidator();
    assertThat(v.validate(ExampleFloatFinite.newBuilder().setVal(Float.NaN).build()).isSuccess())
        .isFalse();
    assertThat(
            v.validate(ExampleFloatFinite.newBuilder().setVal(Float.POSITIVE_INFINITY).build())
                .isSuccess())
        .isFalse();
    assertThat(v.validate(ExampleFloatFinite.newBuilder().setVal(1.0f).build()).isSuccess())
        .isTrue();
  }

  @Test
  void doubleInRuleValueShape() throws ValidationException {
    // 0.0 not in [1.5, 2.5].
    ExampleDoubleIn msg = ExampleDoubleIn.newBuilder().setVal(0.0).build();
    ValidationResult result = nativeValidator().validate(msg);
    assertThat(result.getViolations()).hasSize(1);
    Violation v = result.getViolations().get(0);
    assertThat(v.toProto().getRuleId()).isEqualTo("double.in");

    // For in-list violations the rule_value is the failing value (matches Go's behavior).
    Violation.FieldValue ruleValue = v.getRuleValue();
    assertThat(ruleValue).isNotNull();
    assertThat(ruleValue.getValue()).isEqualTo(0.0);
    FieldDescriptor expectedDesc =
        DoubleRules.getDescriptor().findFieldByNumber(DoubleRules.IN_FIELD_NUMBER);
    assertThat(ruleValue.getDescriptor()).isEqualTo(expectedDesc);
  }

  @Test
  void nativeAndCelProduceEqualViolationProtos() throws ValidationException {
    // Same input, both modes — toProto() must match exactly. Excludes rule_value since that's
    // not in the proto.
    ExampleInt32GtLt msg = ExampleInt32GtLt.newBuilder().setVal(0).build();

    Validator nativeV = nativeValidator();
    Validator celV =
        ValidatorFactory.newBuilder()
            .withConfig(Config.newBuilder().setEnableNativeRules(false).build())
            .build();

    assertThat(nativeV.validate(msg).getViolations().get(0).toProto())
        .isEqualTo(celV.validate(msg).getViolations().get(0).toProto());
  }
}
