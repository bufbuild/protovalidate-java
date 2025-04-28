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

import build.buf.protovalidate.exceptions.ValidationException;
import build.buf.validate.FieldPath;
import build.buf.validate.FieldRules;
import build.buf.validate.Violation;
import com.example.imports.buf.validate.RepeatedRules;
import com.example.imports.validationtest.FieldExpressionMapInt32;
import com.example.imports.validationtest.FieldExpressionRepeatedScalar;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

/** This test verifies that custom (CEL-based) field and/or message rules evaluate as expected. */
public class ValidatorCelExpressionTest {

  @Test
  public void testMap() {
  Map<Integer, Integer> testMap = new HashMap<Integer, Integer>();
  testMap.put(42, 1);
    FieldExpressionMapInt32 msg = FieldExpressionMapInt32.newBuilder().putAllVal(testMap).build();
    Validator validator = new Validator();
    try {
      ValidationResult result = validator.validate(msg);
      System.err.println("Bitch: " + result);
    // assertThat(false).isTrue();
    } catch (ValidationException ve) {
      assertThat(ve).isNull();
    }
  }

  @Test
  public void testList() {
  List<Integer> testList = new ArrayList<Integer>();
  testList.add(1);
    FieldExpressionRepeatedScalar msg = FieldExpressionRepeatedScalar.newBuilder().addAllVal(testList).build();
    Validator validator = new Validator();
    try {
      ValidationResult result = validator.validate(msg);
      System.err.println("Bitch: " + result);
    // assertThat(false).isTrue();
    } catch (ValidationException ve) {
      assertThat(ve).isNull();
    }
  }

  @Test
  public void testFieldExpressionRepeatedMessage() throws Exception {
    // Nested message wrapping the int 1
    com.example.imports.validationtest.FieldExpressionRepeatedMessage.Msg one =
        com.example.imports.validationtest.FieldExpressionRepeatedMessage.Msg.newBuilder()
            .setA(1)
            .build();

    // Nested message wrapping the int 2
    com.example.imports.validationtest.FieldExpressionRepeatedMessage.Msg two =
        com.example.imports.validationtest.FieldExpressionRepeatedMessage.Msg.newBuilder()
            .setA(2)
            .build();

    // Create a valid message (1, 1)
    com.example.imports.validationtest.FieldExpressionRepeatedMessage validMsg =
        com.example.imports.validationtest.FieldExpressionRepeatedMessage.newBuilder()
            .addAllVal(Arrays.asList(one, one))
            .build();

    // Create an invalid message (1, 2, 1)
    com.example.imports.validationtest.FieldExpressionRepeatedMessage invalidMsg =
        com.example.imports.validationtest.FieldExpressionRepeatedMessage.newBuilder()
            .addAllVal(Arrays.asList(one, two, one))
            .build();

    // Build a model of the expected violation
    Violation expectedViolation =
        Violation.newBuilder()
            .setField(
                FieldPath.newBuilder()
                    .addElements(
                        FieldPathUtils.fieldPathElement(
                                invalidMsg.getDescriptorForType().findFieldByName("val"))
                            .toBuilder()
                            .build()))
            .setRule(
                FieldPath.newBuilder()
                    .addElements(
                        FieldPathUtils.fieldPathElement(
                                FieldRules.getDescriptor()
                                    .findFieldByNumber(FieldRules.CEL_FIELD_NUMBER))
                            .toBuilder()
                            .setIndex(0)
                            .build()))
            .setRuleId("field_expression.repeated.message")
            .setMessage("test message field_expression.repeated.message")
            .build();

    Validator validator = new Validator();

    // Valid message checks
    ValidationResult validResult = validator.validate(validMsg);
    assertThat(validResult.isSuccess()).isTrue();

    // Invalid message checks
    ValidationResult invalidResult = validator.validate(invalidMsg);
    assertThat(invalidResult.isSuccess()).isFalse();
    assertThat(invalidResult.toProto().getViolationsList()).containsExactly(expectedViolation);
  }

  @Test
  public void testFieldExpressionRepeatedMessageItems() throws Exception {
    // Nested message wrapping the int 1
    com.example.imports.validationtest.FieldExpressionRepeatedMessageItems.Msg one =
        com.example.imports.validationtest.FieldExpressionRepeatedMessageItems.Msg.newBuilder()
            .setA(1)
            .build();

    // Nested message wrapping the int 2
    com.example.imports.validationtest.FieldExpressionRepeatedMessageItems.Msg two =
        com.example.imports.validationtest.FieldExpressionRepeatedMessageItems.Msg.newBuilder()
            .setA(2)
            .build();

    // Create a valid message (1, 1)
    com.example.imports.validationtest.FieldExpressionRepeatedMessageItems validMsg =
        com.example.imports.validationtest.FieldExpressionRepeatedMessageItems.newBuilder()
            .addAllVal(Arrays.asList(one, one))
            .build();

    // Create an invalid message (1, 2, 1)
    com.example.imports.validationtest.FieldExpressionRepeatedMessageItems invalidMsg =
        com.example.imports.validationtest.FieldExpressionRepeatedMessageItems.newBuilder()
            .addAllVal(Arrays.asList(one, two, one))
            .build();

    // Build a model of the expected violation
    Violation expectedViolation =
        Violation.newBuilder()
            .setField(
                FieldPath.newBuilder()
                    .addElements(
                        FieldPathUtils.fieldPathElement(
                                invalidMsg.getDescriptorForType().findFieldByName("val"))
                            .toBuilder()
                            .setIndex(1)
                            .build()))
            .setRule(
                FieldPath.newBuilder()
                    .addElements(
                        FieldPathUtils.fieldPathElement(
                            FieldRules.getDescriptor()
                                .findFieldByNumber(FieldRules.REPEATED_FIELD_NUMBER)))
                    .addElements(
                        FieldPathUtils.fieldPathElement(
                            RepeatedRules.getDescriptor().findFieldByName("items")))
                    .addElements(
                        FieldPathUtils.fieldPathElement(
                                FieldRules.getDescriptor()
                                    .findFieldByNumber(FieldRules.CEL_FIELD_NUMBER))
                            .toBuilder()
                            .setIndex(0)
                            .build()))
            .setRuleId("field_expression.repeated.message.items")
            .setMessage("test message field_expression.repeated.message.items")
            .build();

    Validator validator = new Validator();

    // Valid message checks
    ValidationResult validResult = validator.validate(validMsg);
    assertThat(validResult.isSuccess()).isTrue();

    // Invalid message checks
    ValidationResult invalidResult = validator.validate(invalidMsg);
    assertThat(invalidResult.isSuccess()).isFalse();
    assertThat(invalidResult.toProto().getViolationsList()).containsExactly(expectedViolation);
  }
}
