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
import com.example.imports.validationtest.FloatDoubleNaNNegZero;
import com.example.noimports.validationtest.ExampleDoubleConstNegZero;
import com.example.noimports.validationtest.ExampleDoubleRepeatedUnique;
import com.example.noimports.validationtest.ExampleFloatConstNegZero;
import com.example.noimports.validationtest.ExampleFloatRepeatedUnique;
import com.google.protobuf.Message;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Comparative tests for the floating-point findings in NATIVE_RULES_REVIEW.md (B1, B2). Each test
 * runs the same input through the native and CEL evaluation paths and asserts on the resulting
 * {@code Violation} protos.
 *
 * <p>Outcomes after running:
 *
 * <ul>
 *   <li><b>B1 — fixed.</b> {@code floatFormatter}/{@code doubleFormatter} now check the sign bit on
 *       entry and return {@code "-0"} for negative zero, so {@code float.const = -0.0} produces the
 *       same violation message in both modes. The tests below lock in that parity.
 *   <li><b>B2 — reclassified.</b> Original review claimed native diverges from CEL on {@code
 *       repeated.unique} for {@code NaN}/{@code -0.0}. Investigation showed CEL's {@code unique()}
 *       is registered by protovalidate-java itself (see {@code CustomOverload.uniqueList}) and uses
 *       {@code Object.equals} on a {@link java.util.HashSet} — the same defect as {@code
 *       RepeatedRulesEvaluator.isUnique}. So both paths agree (both deviate from the CEL spec,
 *       which mandates IEEE-754 equality on doubles). The tests below lock in that agreement so any
 *       future fix has to touch both paths together.
 * </ul>
 */
class FloatBugConfirmationTest {

  private final Validator nativeValidator =
      ValidatorFactory.newBuilder()
          .withConfig(Config.newBuilder().setEnableNativeRules().build())
          .build();
  private final Validator celValidator =
      ValidatorFactory.newBuilder()
          .withConfig(Config.newBuilder().setDisableNativeRules().build())
          .build();

  // --- B1: floatFormatter renders -0.0 as "0", losing the sign --------------------------------

  @Test
  void floatConstNegZero_messageMatchesBetweenNativeAndCel() throws ValidationException {
    // After the floatFormatter fix, both modes report a violation (1.0 != -0.0) with the same
    // message — the rule value is rendered as "-0" in both paths. This test locks in parity;
    // a regression in floatFormatter (e.g. the sign-bit short-circuit being removed) will fail
    // here.
    ExampleFloatConstNegZero msg = ExampleFloatConstNegZero.newBuilder().setVal(1.0f).build();
    String nativeMsg = singleViolationMessage(nativeValidator, msg);
    String celMsg = singleViolationMessage(celValidator, msg);

    assertThat(nativeMsg).isEqualTo("must equal -0");
    assertThat(celMsg).isEqualTo("must equal -0");
  }

  @Test
  void doubleConstNegZero_messageMatchesBetweenNativeAndCel() throws ValidationException {
    ExampleDoubleConstNegZero msg = ExampleDoubleConstNegZero.newBuilder().setVal(1.0).build();
    String nativeMsg = singleViolationMessage(nativeValidator, msg);
    String celMsg = singleViolationMessage(celValidator, msg);

    assertThat(nativeMsg).isEqualTo("must equal -0");
    assertThat(celMsg).isEqualTo("must equal -0");
  }

  // --- B2: repeated.unique on floats — native and CEL agree (both wrong vs spec) --------------
  //
  // CustomOverload.uniqueList (the CEL-side `unique()` registered by protovalidate-java) is
  // implemented with HashSet + Object.equals — the same approach as RepeatedRulesEvaluator's
  // native path. Both treat NaN as duplicate (Java equality) and +0.0/-0.0 as distinct
  // (floatToIntBits), contradicting CEL's spec (IEEE-754: NaN != NaN, +0.0 == -0.0).
  //
  // These tests assert the agreement so any IEEE-754 fix has to ship in CustomOverload.uniqueList
  // and RepeatedRulesEvaluator.isUnique together — otherwise NativeRulesParityTest will start
  // failing.

  @Test
  void floatRepeatedUnique_NaNNaN_bothPathsAgreeBothWrongVsSpec() throws ValidationException {
    ExampleFloatRepeatedUnique msg =
        ExampleFloatRepeatedUnique.newBuilder().addVal(Float.NaN).addVal(Float.NaN).build();
    assertViolationsEqual(msg);
  }

  @Test
  void doubleRepeatedUnique_NaNNaN_bothPathsAgreeBothWrongVsSpec() throws ValidationException {
    ExampleDoubleRepeatedUnique msg =
        ExampleDoubleRepeatedUnique.newBuilder().addVal(Double.NaN).addVal(Double.NaN).build();
    assertViolationsEqual(msg);
  }

  @Test
  void floatRepeatedUnique_PlusZeroMinusZero_bothPathsAgreeBothWrongVsSpec()
      throws ValidationException {
    ExampleFloatRepeatedUnique msg =
        ExampleFloatRepeatedUnique.newBuilder().addVal(0.0f).addVal(-0.0f).build();
    assertViolationsEqual(msg);
  }

  @Test
  void doubleRepeatedUnique_PlusZeroMinusZero_bothPathsAgreeBothWrongVsSpec()
      throws ValidationException {
    ExampleDoubleRepeatedUnique msg =
        ExampleDoubleRepeatedUnique.newBuilder().addVal(0.0).addVal(-0.0).build();
    assertViolationsEqual(msg);
  }

  @Test
  void floatDoubleNaNNegZero() throws ValidationException {
    // these tests are also checking that an unset (zero) field is equal to -0
    FloatDoubleNaNNegZero nanMsg =
        FloatDoubleNaNNegZero.newBuilder()
            .addDvals(Double.NaN)
            .addDvals(Double.NaN)
            .addFvals(Float.NaN)
            .addFvals(Float.NaN)
            .build();
    // should both be no error, since NaN is not equal to itself
    // it's not because Java CEL is broken so replicate broken behavior
    ValidationResult nanMsgResultNative = nativeValidator.validate(nanMsg);
    ValidationResult nanMsgResultCEL = celValidator.validate(nanMsg);
    assertViolationsEqual(nanMsg);
    assertThat(nanMsgResultNative.getViolations()).isNotEmpty();
    assertThat(nanMsgResultCEL.getViolations()).isNotEmpty();

    // now check -0 and 0 for uniqueness (should not be)
    FloatDoubleNaNNegZero zeroMsg =
        FloatDoubleNaNNegZero.newBuilder()
            .addDvals(0.0)
            .addDvals(-0.0)
            .addFvals(0.0F)
            .addFvals(-0.0F)
            .build();
    // should both be error, since 0 == -0
    // but it's not because Java CEL is broken on unique tests for -0 so replicate broken behavior
    nanMsgResultNative = nativeValidator.validate(zeroMsg);
    nanMsgResultCEL = celValidator.validate(zeroMsg);
    assertViolationsEqual(zeroMsg);
    assertThat(nanMsgResultNative.getViolations()).isEmpty();
    assertThat(nanMsgResultCEL.getViolations()).isEmpty();
  }

  // --- helpers ----------------------------------------------------------------------------------

  private static String singleViolationMessage(Validator v, Message msg)
      throws ValidationException {
    ValidationResult result = v.validate(msg);
    List<String> messages =
        result.getViolations().stream()
            .map(violation -> violation.toProto().getMessage())
            .collect(Collectors.toList());
    assertThat(messages).hasSize(1);
    return messages.get(0);
  }

  private void assertViolationsEqual(Message msg) throws ValidationException {
    List<build.buf.validate.Violation> nativeProtos = toProtoList(nativeValidator.validate(msg));
    List<build.buf.validate.Violation> celProtos = toProtoList(celValidator.validate(msg));
    assertThat(nativeProtos)
        .as("native and CEL must produce identical Violation protos for %s", msg)
        .isEqualTo(celProtos);
  }

  private static List<build.buf.validate.Violation> toProtoList(ValidationResult result) {
    return result.getViolations().stream().map(v -> v.toProto()).collect(Collectors.toList());
  }
}
