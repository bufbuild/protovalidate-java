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
import build.buf.validate.BoolRules;
import com.example.noimports.validationtest.ExampleBoolConst;
import com.google.protobuf.Descriptors.FieldDescriptor;
import org.junit.jupiter.api.Test;

/**
 * Validator-level integration tests for {@link BoolRulesEvaluator}. Mirrors the per-rule tests in
 * protovalidate-go's {@code native_bool_test.go}, plus an explicit assertion on {@link
 * Violation#getRuleValue()} since the conformance suite cannot detect divergence on that field (it
 * is not part of the {@code Violation} proto schema — see CHANGELOG Phase 1).
 */
class BoolRulesEvaluatorTest {

  private static Validator nativeValidator() {
    Config config = Config.newBuilder().setDisableNativeRules(false).build();
    return ValidatorFactory.newBuilder().withConfig(config).build();
  }

  @Test
  void boolConstPasses() throws ValidationException {
    Validator validator = nativeValidator();
    ExampleBoolConst msg = ExampleBoolConst.newBuilder().setFlag(true).build();
    ValidationResult result = validator.validate(msg);
    assertThat(result.getViolations()).isEmpty();
    assertThat(result.isSuccess()).isTrue();
  }

  @Test
  void boolConstFailsAndCarriesExpectedViolationShape() throws ValidationException {
    Validator validator = nativeValidator();
    // Default value (false) fails the const=true rule. Bool fields without explicit-presence in
    // proto3 still apply rules at default; bool has no IGNORE_IF_ZERO_VALUE behavior here.
    ExampleBoolConst msg = ExampleBoolConst.newBuilder().setFlag(false).build();
    ValidationResult result = validator.validate(msg);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.getViolations()).hasSize(1);

    Violation violation = result.getViolations().get(0);
    build.buf.validate.Violation proto = violation.toProto();
    assertThat(proto.getRuleId()).isEqualTo("bool.const");
    assertThat(proto.getMessage()).isEqualTo("must equal true");
    assertThat(proto.getField().getElementsList()).hasSize(1);
    assertThat(proto.getField().getElements(0).getFieldName()).isEqualTo("flag");
    assertThat(proto.getRule().getElementsList()).hasSize(2);
    assertThat(proto.getRule().getElements(0).getFieldName()).isEqualTo("bool");
    assertThat(proto.getRule().getElements(1).getFieldName()).isEqualTo("const");

    // getRuleValue is NOT in the Violation proto — it's only on the Java wrapper. Conformance
    // can't catch divergence on this field, so assert it explicitly. See Phase 1 CHANGELOG.
    Violation.FieldValue ruleValue = violation.getRuleValue();
    assertThat(ruleValue).isNotNull();
    assertThat(ruleValue.getValue()).isEqualTo(true);
    FieldDescriptor expectedRuleDesc =
        BoolRules.getDescriptor().findFieldByNumber(BoolRules.CONST_FIELD_NUMBER);
    assertThat(ruleValue.getDescriptor()).isEqualTo(expectedRuleDesc);

    Violation.FieldValue fieldValue = violation.getFieldValue();
    assertThat(fieldValue).isNotNull();
    assertThat(fieldValue.getValue()).isEqualTo(false);
  }

  @Test
  void nativeAndCelProducePartiallyEqualViolations() throws ValidationException {
    // Same input, both modes — toProto() must match exactly. (rule_value isn't in the proto so
    // CEL/native can disagree there; that's covered by the dedicated assertion above.)
    ExampleBoolConst msg = ExampleBoolConst.newBuilder().setFlag(false).build();

    ValidationResult nativeResult = nativeValidator().validate(msg);
    Validator celValidator =
        ValidatorFactory.newBuilder()
            .withConfig(Config.newBuilder().setDisableNativeRules(true).build())
            .build();
    ValidationResult celResult = celValidator.validate(msg);

    assertThat(nativeResult.getViolations()).hasSize(1);
    assertThat(celResult.getViolations()).hasSize(1);
    assertThat(nativeResult.getViolations().get(0).toProto())
        .isEqualTo(celResult.getViolations().get(0).toProto());
  }

  @Test
  void nativeDispatchClearsRuleSoCelDoesNotDuplicateIt() throws ValidationException {
    // If the dispatcher failed to clear bool.const on the residual FieldRules, CEL would also
    // produce a violation and we'd see two. One violation proves clone-and-clear works.
    ExampleBoolConst msg = ExampleBoolConst.newBuilder().setFlag(false).build();
    ValidationResult result = nativeValidator().validate(msg);
    assertThat(result.getViolations()).hasSize(1);
  }
}
