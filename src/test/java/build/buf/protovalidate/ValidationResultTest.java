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

import build.buf.validate.FieldPathElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class ValidationResultTest {
  @Test
  void testToStringNoViolations() {

    List<RuleViolation> violations = new ArrayList<>();
    ValidationResult result = new ValidationResult(violations);

    assertThat(result.toString()).isEqualTo("Validation OK");
    assertThat(result.isSuccess()).isTrue();
  }

  @Test
  void testToStringSingleViolation() {
    FieldPathElement elem =
        FieldPathElement.newBuilder().setFieldNumber(5).setFieldName("test_field_name").build();

    RuleViolation violation =
        RuleViolation.newBuilder()
            .setRuleId("int32.const")
            .setMessage("must equal 42")
            .addFirstFieldPathElement(elem)
            .build();
    ValidationResult result = new ValidationResult(Collections.singletonList(violation));

    assertThat(result.toString())
        .isEqualTo("Validation error:\n - test_field_name: must equal 42 [int32.const]");
  }

  @Test
  void testToStringMultipleViolations() {
    FieldPathElement elem =
        FieldPathElement.newBuilder().setFieldNumber(5).setFieldName("test_field_name").build();

    RuleViolation violation1 =
        RuleViolation.newBuilder()
            .setRuleId("int32.const")
            .setMessage("must equal 42")
            .addFirstFieldPathElement(elem)
            .build();

    RuleViolation violation2 =
        RuleViolation.newBuilder()
            .setRuleId("int32.required")
            .setMessage("value is required")
            .addFirstFieldPathElement(elem)
            .build();
    ValidationResult result = new ValidationResult(Arrays.asList(violation1, violation2));

    assertThat(result.toString())
        .isEqualTo(
            "Validation error:\n - test_field_name: must equal 42 [int32.const]\n - test_field_name: value is required [int32.required]");
  }

  @Test
  void testToStringSingleViolationMultipleFieldPathElements() {
    FieldPathElement elem1 =
        FieldPathElement.newBuilder().setFieldNumber(5).setFieldName("test_field_name").build();
    FieldPathElement elem2 =
        FieldPathElement.newBuilder().setFieldNumber(5).setFieldName("nested_name").build();

    RuleViolation violation1 =
        RuleViolation.newBuilder()
            .setRuleId("int32.const")
            .setMessage("must equal 42")
            .addAllFieldPathElements(Arrays.asList(elem1, elem2))
            .build();

    ValidationResult result = new ValidationResult(Collections.singletonList(violation1));

    assertThat(result.toString())
        .isEqualTo(
            "Validation error:\n - test_field_name.nested_name: must equal 42 [int32.const]");
  }

  @Test
  void testToStringSingleViolationNoFieldPathElements() {
    RuleViolation violation =
        RuleViolation.newBuilder().setRuleId("int32.const").setMessage("must equal 42").build();
    ValidationResult result = new ValidationResult(Collections.singletonList(violation));

    assertThat(result.toString()).isEqualTo("Validation error:\n - must equal 42 [int32.const]");
  }
}
