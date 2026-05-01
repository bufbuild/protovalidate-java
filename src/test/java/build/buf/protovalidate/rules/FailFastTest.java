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
import com.example.noimports.validationtest.BytesMultiRule;
import com.example.noimports.validationtest.Int32MultiRule;
import com.example.noimports.validationtest.StringMultiRule;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import org.junit.jupiter.api.Test;

/**
 * failFast tests for the native rule evaluators. Each fixture is constructed so the input
 * violates two rules. Without failFast, both violations are reported; with failFast=true, the
 * validator must short-circuit after the first.
 */
class FailFastTest {

  private static Validator validator(boolean failFast) {
    Config config = Config.newBuilder().setEnableNativeRules(true).setFailFast(failFast).build();
    return ValidatorFactory.newBuilder().withConfig(config).build();
  }

  @Test
  void stringEvaluator_failFastSkipsLaterRules() throws ValidationException {
    // "ab" — fails min_len=4, would also fail pattern .*[0-9].*
    StringMultiRule msg = StringMultiRule.newBuilder().setVal("ab").build();
    assertTwoViolationsWithoutFailFastOneWith(msg);
  }

  @Test
  void numericEvaluator_failFastSkipsLaterRules() throws ValidationException {
    // val=0: fails const=5 and fails gt=10
    Int32MultiRule msg = Int32MultiRule.newBuilder().setVal(0).build();
    assertTwoViolationsWithoutFailFastOneWith(msg);
  }

  @Test
  void bytesEvaluator_failFastSkipsLaterRules() throws ValidationException {
    // 1-byte value — fails min_len=4 AND fails ipv4 size requirement.
    BytesMultiRule msg =
        BytesMultiRule.newBuilder().setVal(ByteString.copyFrom(new byte[] {0x01})).build();
    assertTwoViolationsWithoutFailFastOneWith(msg);
  }

  private void assertTwoViolationsWithoutFailFastOneWith(Message msg) throws ValidationException {
    ValidationResult full = validator(false).validate(msg);
    ValidationResult fast = validator(true).validate(msg);
    assertThat(full.getViolations())
        .as("without failFast, both violations should be reported")
        .hasSize(2);
    assertThat(fast.getViolations())
        .as("with failFast, only the first violation should be reported")
        .hasSize(1);
  }
}
