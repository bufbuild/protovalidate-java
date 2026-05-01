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
import com.example.noimports.validationtest.BytesNotIn;
import com.example.noimports.validationtest.EnumNotIn;
import com.example.noimports.validationtest.ExampleColor;
import com.example.noimports.validationtest.Int32NotIn;
import com.example.noimports.validationtest.StringNotIn;
import com.example.noimports.validationtest.Uint32NotIn;
import com.google.protobuf.ByteString;
import org.junit.jupiter.api.Test;

/**
 * Targeted not_in coverage for each native evaluator. The conformance suite covers behavior; what
 * these tests pin down is the rule_id and message text the native path produces, since those are
 * the contract observable from {@code Violation.toProto()}.
 */
class NotInRulesTest {

  private final Validator validator =
      ValidatorFactory.newBuilder()
          .withConfig(Config.newBuilder().setEnableNativeRules(true).build())
          .build();

  @Test
  void int32NotIn() throws ValidationException {
    Int32NotIn msg = Int32NotIn.newBuilder().setVal(2).build();
    ValidationResult result = validator.validate(msg);
    assertThat(result.getViolations()).hasSize(1);
    assertThat(result.getViolations().get(0).toProto().getRuleId()).isEqualTo("int32.not_in");
    assertThat(result.getViolations().get(0).toProto().getMessage())
        .isEqualTo("must not be in list [1, 2, 3]");
  }

  @Test
  void uint32NotIn() throws ValidationException {
    Uint32NotIn msg = Uint32NotIn.newBuilder().setVal(1).build();
    ValidationResult result = validator.validate(msg);
    assertThat(result.getViolations()).hasSize(1);
    assertThat(result.getViolations().get(0).toProto().getRuleId()).isEqualTo("uint32.not_in");
  }

  @Test
  void stringNotIn() throws ValidationException {
    StringNotIn msg = StringNotIn.newBuilder().setVal("foo").build();
    ValidationResult result = validator.validate(msg);
    assertThat(result.getViolations()).hasSize(1);
    assertThat(result.getViolations().get(0).toProto().getRuleId()).isEqualTo("string.not_in");
    assertThat(result.getViolations().get(0).toProto().getMessage())
        .isEqualTo("must not be in list [foo, bar]");
  }

  @Test
  void bytesNotIn() throws ValidationException {
    BytesNotIn msg = BytesNotIn.newBuilder().setVal(ByteString.copyFromUtf8("AA")).build();
    ValidationResult result = validator.validate(msg);
    assertThat(result.getViolations()).hasSize(1);
    assertThat(result.getViolations().get(0).toProto().getRuleId()).isEqualTo("bytes.not_in");
  }

  @Test
  void enumNotIn() throws ValidationException {
    EnumNotIn msg = EnumNotIn.newBuilder().setVal(ExampleColor.EXAMPLE_COLOR_RED).build();
    ValidationResult result = validator.validate(msg);
    assertThat(result.getViolations()).hasSize(1);
    assertThat(result.getViolations().get(0).toProto().getRuleId()).isEqualTo("enum.not_in");
    assertThat(result.getViolations().get(0).toProto().getMessage())
        .isEqualTo("must not be in list [1, 2]");
  }
}
