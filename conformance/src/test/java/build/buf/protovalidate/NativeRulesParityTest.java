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
import build.buf.validate.conformance.cases.AnEnum;
import build.buf.validate.conformance.cases.BoolConstTrue;
import build.buf.validate.conformance.cases.BytesContains;
import build.buf.validate.conformance.cases.BytesIn;
import build.buf.validate.conformance.cases.ComplexTestMsg;
import build.buf.validate.conformance.cases.EnumDefined;
import build.buf.validate.conformance.cases.Fixed32LT;
import build.buf.validate.conformance.cases.Int32In;
import build.buf.validate.conformance.cases.KitchenSinkMessage;
import build.buf.validate.conformance.cases.RepeatedEnumIn;
import build.buf.validate.conformance.cases.RepeatedExact;
import build.buf.validate.conformance.cases.RepeatedUnique;
import build.buf.validate.conformance.cases.SFixed64In;
import build.buf.validate.conformance.cases.StringContains;
import build.buf.validate.conformance.cases.StringLen;
import build.buf.validate.conformance.cases.StringPrefix;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Parity test: runs a representative slice of conformance fixtures through both modes
 * ({@code disableNativeRules=true} and {@code false}) and asserts the resulting
 * {@code Violation} protos are byte-equal. The conformance suite proves each mode is correct in
 * isolation; this test proves they don't drift from each other on the same input.
 *
 * <p>Conformance message text is excluded from the suite's default comparison (only
 * {@code rule_id}, {@code field}, {@code rule}, {@code for_key} are compared unless
 * {@code --strict_message} is set), but {@code toProto()} captures all of those plus the
 * message text. Asserting full {@code toProto()} equality here is therefore stricter than
 * conformance.
 */
class NativeRulesParityTest {

  private final Validator nativeValidator =
      ValidatorFactory.newBuilder()
          .withConfig(Config.newBuilder().setDisableNativeRules(false).build())
          .build();
  private final Validator celValidator =
      ValidatorFactory.newBuilder()
          .withConfig(Config.newBuilder().setDisableNativeRules(true).build())
          .build();

  @Test
  void parityAcrossRuleTypes() throws ValidationException {
    // Each fixture exercises a different rule type. Order:
    // bool, enum, bytes, numeric (signed + unsigned), string (scalar + format), repeated, map.
    List<Message> fixtures =
        Arrays.asList(
            // Bool — fails const=true.
            BoolConstTrue.newBuilder().build(),
            // Enum defined_only — fails (2147483647 not in defined values).
            EnumDefined.newBuilder().setValValue(2147483647).build(),
            // Bytes contains — pass case.
            BytesContains.newBuilder().setVal(ByteString.copyFromUtf8("candy bars")).build(),
            // Bytes in — pass case (empty matches none of the in list, but the field is
            // implicit-presence so the rule is skipped on empty value).
            BytesIn.newBuilder().setVal(ByteString.copyFromUtf8("bar")).build(),
            // Fixed32 (unsigned) lt — fails (val=5, lt=5).
            Fixed32LT.newBuilder().setVal(5).build(),
            // Int32 in — fails (4 not in list).
            Int32In.newBuilder().setVal(4).build(),
            // SFixed64 in — fails (5 not in list).
            SFixed64In.newBuilder().setVal(5).build(),
            // String prefix — pass case.
            StringPrefix.newBuilder().setVal("foo").build(),
            // String contains — pass case.
            StringContains.newBuilder().setVal("foobar").build(),
            // String length with code points — emoji counts as 1 each.
            StringLen.newBuilder().setVal("😅😄👾").build(),
            // Repeated exact — fails (2 items, exact=3).
            RepeatedExact.newBuilder().addAllVal(Arrays.asList(1, 2)).build(),
            // Repeated unique — fails (duplicate "foo").
            RepeatedUnique.newBuilder()
                .addAllVal(Arrays.asList("foo", "bar", "foo", "baz"))
                .build(),
            // Repeated enum in — fails.
            RepeatedEnumIn.newBuilder().addVal(AnEnum.AN_ENUM_X).build(),
            // KitchenSinkMessage with empty inner ComplexTestMsg — many violations.
            KitchenSinkMessage.newBuilder().setVal(ComplexTestMsg.newBuilder().build()).build());

    for (Message msg : fixtures) {
      ValidationResult nativeResult = nativeValidator.validate(msg);
      ValidationResult celResult = celValidator.validate(msg);
      assertThat(toProtoList(nativeResult))
          .as("toProto() parity failed for %s", msg.getDescriptorForType().getFullName())
          .isEqualTo(toProtoList(celResult));
    }
  }

  private static List<build.buf.validate.Violation> toProtoList(ValidationResult result) {
    return result.getViolations().stream()
        .map(Violation::toProto)
        .collect(Collectors.toList());
  }
}
