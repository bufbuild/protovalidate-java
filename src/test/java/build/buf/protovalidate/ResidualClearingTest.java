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
import com.example.noimports.validationtest.ExampleBoolConst;
import com.example.noimports.validationtest.ExampleBytesConst;
import com.example.noimports.validationtest.ExampleEnumConst;
import com.example.noimports.validationtest.ExampleInt32Const;
import com.example.noimports.validationtest.ExampleMapMinMax;
import com.example.noimports.validationtest.ExampleRepeatedMinMax;
import com.example.noimports.validationtest.ExampleStringConst;
import com.google.protobuf.Message;
import org.junit.jupiter.api.Test;

/**
 * Residual-clearing contract tests. When a native evaluator handles a rule, the dispatcher must
 * clear that rule on the residual {@code FieldRules} so {@code RuleCache} doesn't compile a CEL
 * program that fires a duplicate violation. Each test below uses a fixture that fails exactly one
 * native rule and asserts that the validator produces exactly one violation, not two.
 */
class ResidualClearingTest {

  private final Validator nativeValidator =
      ValidatorFactory.newBuilder()
          .withConfig(Config.newBuilder().setEnableNativeRules().build())
          .build();

  @Test
  void boolConstFiresOnce() throws ValidationException {
    assertExactlyOneViolation(ExampleBoolConst.newBuilder().setFlag(false).build());
  }

  @Test
  void int32ConstFiresOnce() throws ValidationException {
    assertExactlyOneViolation(ExampleInt32Const.newBuilder().setVal(0).build());
  }

  @Test
  void enumConstFiresOnce() throws ValidationException {
    assertExactlyOneViolation(ExampleEnumConst.newBuilder().build());
  }

  @Test
  void bytesConstFiresOnce() throws ValidationException {
    assertExactlyOneViolation(ExampleBytesConst.newBuilder().build());
  }

  @Test
  void stringConstFiresOnce() throws ValidationException {
    assertExactlyOneViolation(ExampleStringConst.newBuilder().setVal("nope").build());
  }

  @Test
  void repeatedMinItemsFiresOnce() throws ValidationException {
    assertExactlyOneViolation(ExampleRepeatedMinMax.newBuilder().build());
  }

  @Test
  void mapMinPairsFiresOnce() throws ValidationException {
    assertExactlyOneViolation(ExampleMapMinMax.newBuilder().build());
  }

  private void assertExactlyOneViolation(Message msg) throws ValidationException {
    ValidationResult result = nativeValidator.validate(msg);
    assertThat(result.getViolations())
        .as(
            "native dispatcher must clear the rule from the residual; expected exactly one "
                + "violation but got: %s",
            result.getViolations())
        .hasSize(1);
  }
}
