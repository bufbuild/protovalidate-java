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
import com.example.noimports.validationtest.DoubleRuleOrder;
import com.example.noimports.validationtest.EnumConstDefinedOnlyOrder;
import com.example.noimports.validationtest.EnumRuleOrder;
import com.example.noimports.validationtest.Int32RuleOrder;
import com.example.noimports.validationtest.MapStringWrapperValues;
import com.example.noimports.validationtest.MapWrapperValues;
import com.example.noimports.validationtest.MinItemsWrappers;
import com.example.noimports.validationtest.RepeatedWrapperItems;
import com.example.noimports.validationtest.RepeatedWrapperStandardAndCustom;
import com.example.noimports.validationtest.StringRuleOrder;
import com.example.noimports.validationtest.UniqueBytesWrappers;
import com.example.noimports.validationtest.UniqueWrappers;
import com.example.noimports.validationtest.WrapperCustomOnly;
import com.example.noimports.validationtest.WrapperStandardAndCelExpression;
import com.example.noimports.validationtest.WrapperStandardAndCustom;
import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Int32Value;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Regression tests for wrapper handling and violation ordering. Every case runs with native rules
 * enabled and disabled; both paths must agree.
 */
class NativeRulesRegressionTest {

  private static Validator validator(boolean nativeRules, boolean failFast) {
    Config config =
        Config.newBuilder().setEnableNativeRules(nativeRules).setFailFast(failFast).build();
    return ValidatorFactory.newBuilder().withConfig(config).build();
  }

  private static List<String> ruleIds(ValidationResult result) {
    return result.getViolations().stream()
        .map(v -> v.toProto().getRuleId())
        .collect(Collectors.toList());
  }

  private static List<Int32Value> int32s(int... vals) {
    return Arrays.stream(vals).mapToObj(Int32Value::of).collect(Collectors.toList());
  }

  private static Message toDynamic(Message msg) throws InvalidProtocolBufferException {
    return DynamicMessage.parseFrom(msg.getDescriptorForType(), msg.toByteString());
  }

  static Stream<Arguments> wrapperCases() {
    return Stream.of(
        Arguments.of(
            "repeated_items/invalid",
            RepeatedWrapperItems.newBuilder().addAllVal(int32s(5)).build(),
            "int32.gt",
            "val[0]"),
        Arguments.of(
            "repeated_items/valid",
            RepeatedWrapperItems.newBuilder().addAllVal(int32s(100)).build(),
            null,
            null),
        Arguments.of(
            "map_int32/invalid",
            MapWrapperValues.newBuilder().putVal("key", Int32Value.of(5)).build(),
            "int32.gt",
            "val[\"key\"]"),
        Arguments.of(
            "map_int32/valid",
            MapWrapperValues.newBuilder().putVal("key", Int32Value.of(100)).build(),
            null,
            null),
        Arguments.of(
            "map_string/invalid",
            MapStringWrapperValues.newBuilder().putVal("k", StringValue.of("a")).build(),
            "string.min_len",
            "val[\"k\"]"),
        Arguments.of(
            "map_string/valid",
            MapStringWrapperValues.newBuilder().putVal("k", StringValue.of("abc")).build(),
            null,
            null),
        Arguments.of(
            "min_items/invalid",
            MinItemsWrappers.newBuilder().build(),
            "repeated.min_items",
            "val"),
        Arguments.of(
            "min_items/valid",
            MinItemsWrappers.newBuilder().addAllVal(int32s(1, 2)).build(),
            null,
            null),
        Arguments.of(
            "unique/invalid",
            UniqueWrappers.newBuilder().addAllVal(int32s(1, 1)).build(),
            "repeated.unique",
            "val"),
        Arguments.of(
            "unique/valid",
            UniqueWrappers.newBuilder().addAllVal(int32s(1, 2)).build(),
            null,
            null),
        Arguments.of(
            "unique_bytes/invalid",
            UniqueBytesWrappers.newBuilder()
                .addVal(BytesValue.of(ByteString.copyFromUtf8("a")))
                .addVal(BytesValue.of(ByteString.copyFromUtf8("a")))
                .build(),
            "repeated.unique",
            "val"),
        Arguments.of(
            "unique_bytes/valid",
            UniqueBytesWrappers.newBuilder()
                .addVal(BytesValue.of(ByteString.copyFromUtf8("a")))
                .addVal(BytesValue.of(ByteString.copyFromUtf8("b")))
                .build(),
            null,
            null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("wrapperCases")
  void wrappersInCollections(
      String name, Message msg, @Nullable String wantRule, @Nullable String wantPath)
      throws Exception {
    for (Message candidate : Arrays.asList(msg, toDynamic(msg))) {
      ValidationResult nativeResult = validator(true, false).validate(candidate);
      ValidationResult celResult = validator(false, false).validate(candidate);
      if (wantRule == null) {
        assertThat(nativeResult.isSuccess()).isTrue();
        assertThat(celResult.isSuccess()).isTrue();
        continue;
      }
      assertThat(nativeResult.getViolations()).hasSize(1);
      build.buf.validate.Violation violation = nativeResult.getViolations().get(0).toProto();
      assertThat(violation.getRuleId()).isEqualTo(wantRule);
      assertThat(FieldPathUtils.fieldPathString(violation.getField())).isEqualTo(wantPath);
      assertThat(nativeResult.toProto()).isEqualTo(celResult.toProto());
    }
  }

  static Stream<Arguments> wrapperRulesNotDuplicatedCases() {
    return Stream.of(
        Arguments.of(
            "standard_and_custom/both_fail",
            WrapperStandardAndCustom.newBuilder().setVal(Int32Value.of(5)).build(),
            Arrays.asList("custom", "int32.gt")),
        Arguments.of(
            "standard_and_custom/custom_fails",
            WrapperStandardAndCustom.newBuilder().setVal(Int32Value.of(50)).build(),
            Arrays.asList("custom")),
        Arguments.of(
            "custom_only",
            WrapperCustomOnly.newBuilder().setVal(Int32Value.of(5)).build(),
            Arrays.asList("custom")),
        Arguments.of(
            "repeated_items/both_fail",
            RepeatedWrapperStandardAndCustom.newBuilder().addAllVal(int32s(5)).build(),
            Arrays.asList("custom", "int32.gt")),
        Arguments.of(
            "standard_and_cel_expression/both_fail",
            WrapperStandardAndCelExpression.newBuilder().setVal(Int32Value.of(5)).build(),
            Arrays.asList("this > 100 ? '' : 'must be greater than 100'", "int32.gt")));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("wrapperRulesNotDuplicatedCases")
  void wrapperFieldRulesNotDuplicated(String name, Message msg, List<String> want)
      throws ValidationException {
    for (boolean nativeRules : new boolean[] {true, false}) {
      assertThat(ruleIds(validator(nativeRules, false).validate(msg)))
          .as("nativeRules=%s", nativeRules)
          .isEqualTo(want);
    }
  }

  static Stream<Arguments> violationOrderCases() {
    return Stream.of(
        Arguments.of(
            "int32",
            Int32RuleOrder.newBuilder().setVal(3).build(),
            Arrays.asList("int32.gt", "int32.in", "int32.not_in")),
        Arguments.of(
            "double",
            DoubleRuleOrder.newBuilder().setVal(Double.NEGATIVE_INFINITY).build(),
            Arrays.asList("double.gt", "double.in", "double.not_in", "double.finite")),
        Arguments.of(
            "string",
            StringRuleOrder.newBuilder().setVal("x").build(),
            Arrays.asList(
                "string.const", "string.len", "string.min_bytes", "string.prefix", "string.in")),
        Arguments.of(
            "enum",
            EnumRuleOrder.newBuilder().setValValue(99).build(),
            Arrays.asList("enum.defined_only", "enum.in", "enum.not_in")),
        Arguments.of(
            "enum_const",
            EnumConstDefinedOnlyOrder.newBuilder().setValValue(99).build(),
            Arrays.asList("enum.const", "enum.defined_only", "enum.in")));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("violationOrderCases")
  void violationsFollowValidateProtoOrder(String name, Message msg, List<String> want)
      throws Exception {
    for (boolean nativeRules : new boolean[] {true, false}) {
      for (Message candidate : Arrays.asList(msg, toDynamic(msg))) {
        assertThat(ruleIds(validator(nativeRules, false).validate(candidate)))
            .as("nativeRules=%s", nativeRules)
            .isEqualTo(want);
        assertThat(ruleIds(validator(nativeRules, true).validate(candidate)))
            .as("nativeRules=%s failFast", nativeRules)
            .containsExactly(want.get(0));
      }
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void violationOrderMatchesBetweenNativeAndCel(boolean failFast) throws ValidationException {
    StringRuleOrder msg = StringRuleOrder.newBuilder().setVal("x").build();
    assertThat(validator(true, failFast).validate(msg).toProto())
        .isEqualTo(validator(false, failFast).validate(msg).toProto());
  }
}
