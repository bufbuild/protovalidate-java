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
import build.buf.validate.EnumRules;
import com.example.noimports.validationtest.ExampleColor;
import com.example.noimports.validationtest.ExampleEnumConst;
import com.example.noimports.validationtest.ExampleEnumIn;
import com.google.protobuf.Descriptors.FieldDescriptor;
import org.junit.jupiter.api.Test;

/** Validator-level tests for {@link EnumRulesEvaluator}. */
class EnumRulesEvaluatorTest {

  private static Validator nativeValidator() {
    Config config = Config.newBuilder().setEnableNativeRules().build();
    return ValidatorFactory.newBuilder().withConfig(config).build();
  }

  @Test
  void enumConstFailsAndCarriesExpectedShape() throws ValidationException {
    // Default UNSPECIFIED (0) != const 2 (GREEN).
    ExampleEnumConst msg = ExampleEnumConst.newBuilder().build();
    ValidationResult result = nativeValidator().validate(msg);
    assertThat(result.getViolations()).hasSize(1);
    Violation v = result.getViolations().get(0);

    build.buf.validate.Violation proto = v.toProto();
    assertThat(proto.getRuleId()).isEqualTo("enum.const");
    assertThat(proto.getMessage()).isEqualTo("must equal 2");
    assertThat(proto.getRule().getElements(0).getFieldName()).isEqualTo("enum");
    assertThat(proto.getRule().getElements(1).getFieldName()).isEqualTo("const");

    // rule_value isn't in the Violation proto — assert it directly here. See Phase 1 CHANGELOG.
    Violation.FieldValue ruleValue = v.getRuleValue();
    assertThat(ruleValue).isNotNull();
    assertThat(ruleValue.getValue()).isEqualTo(2);
    FieldDescriptor expectedDesc =
        EnumRules.getDescriptor().findFieldByNumber(EnumRules.CONST_FIELD_NUMBER);
    assertThat(ruleValue.getDescriptor()).isEqualTo(expectedDesc);
  }

  @Test
  void enumInFailsForValueNotInList() throws ValidationException {
    // Default UNSPECIFIED (0) not in [1, 3].
    ExampleEnumIn msg = ExampleEnumIn.newBuilder().build();
    ValidationResult result = nativeValidator().validate(msg);
    assertThat(result.getViolations()).hasSize(1);
    build.buf.validate.Violation proto = result.getViolations().get(0).toProto();
    assertThat(proto.getRuleId()).isEqualTo("enum.in");
    assertThat(proto.getMessage()).isEqualTo("must be in list [1, 3]");
  }

  @Test
  void enumInPassesForValueInList() throws ValidationException {
    ExampleEnumIn msg = ExampleEnumIn.newBuilder().setVal(ExampleColor.EXAMPLE_COLOR_RED).build();
    ValidationResult result = nativeValidator().validate(msg);
    assertThat(result.isSuccess()).isTrue();
  }

  @Test
  void nativeAndCelProduceEqualViolationProto() throws ValidationException {
    ExampleEnumConst msg = ExampleEnumConst.newBuilder().build();
    Validator nativeV = nativeValidator();
    Validator celV =
        ValidatorFactory.newBuilder()
            .withConfig(Config.newBuilder().setDisableNativeRules().build())
            .build();
    assertThat(nativeV.validate(msg).getViolations().get(0).toProto())
        .isEqualTo(celV.validate(msg).getViolations().get(0).toProto());
  }
}
