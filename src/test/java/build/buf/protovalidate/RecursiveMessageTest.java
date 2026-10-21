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

import build.buf.validate.FieldRules;
import build.buf.validate.Int32Rules;
import build.buf.validate.RepeatedRules;
import build.buf.validate.StringRules;
import build.buf.validate.ValidateProto;
import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldOptions;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.ListValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Message evaluators are cached before they are built so recursive references can resolve to them.
 * These tests cover references made while the referenced evaluator is still incomplete.
 */
public class RecursiveMessageTest {
  private static final FieldOptions INT32_GT_100 =
      rules(FieldRules.newBuilder().setInt32(Int32Rules.newBuilder().setGt(100)));

  @Test
  public void testMutuallyRecursiveMessagesValidateEveryDepth() throws Exception {
    Map<String, Descriptor> types = mutuallyRecursiveTypes();
    Descriptor a = types.get("A");
    Descriptor b = types.get("B");
    Validator validator = ValidatorFactory.newBuilder().build();
    assertThat(validator.validate(nestedA(a, b, 2, 5)).getViolations()).hasSize(2);
    assertThat(validator.validate(nestedA(a, b, 3, 5)).getViolations()).hasSize(3);
  }

  @Test
  public void testMutuallyRecursiveMessagesValidateFromEitherRoot() throws Exception {
    Map<String, Descriptor> types = mutuallyRecursiveTypes();
    Descriptor a = types.get("A");
    Descriptor b = types.get("B");
    DynamicMessage root =
        DynamicMessage.newBuilder(b)
            .setField(b.findFieldByName("v"), 5)
            .setField(b.findFieldByName("a"), nestedA(a, b, 1, 5))
            .build();
    Validator validator = ValidatorFactory.newBuilder().build();
    assertThat(validator.validate(root).getViolations()).hasSize(2);
  }

  @Test
  public void testMutuallyRecursiveMessagesAcceptValidValues() throws Exception {
    Map<String, Descriptor> types = mutuallyRecursiveTypes();
    Validator validator = ValidatorFactory.newBuilder().build();
    assertThat(validator.validate(nestedA(types.get("A"), types.get("B"), 2, 101)).isSuccess())
        .isTrue();
  }

  @Test
  public void testSelfRecursiveFieldDeclaredBeforeRule() throws Exception {
    Descriptor node =
        compile(
                "recursive",
                DescriptorProto.newBuilder()
                    .setName("Node")
                    .addField(messageField("next", 1, ".recursive.Node"))
                    .addField(
                        field("v", 2, FieldDescriptorProto.Type.TYPE_INT32)
                            .setOptions(INT32_GT_100)))
            .get("Node");
    DynamicMessage inner =
        DynamicMessage.newBuilder(node).setField(node.findFieldByName("v"), 5).build();
    DynamicMessage root =
        DynamicMessage.newBuilder(node)
            .setField(node.findFieldByName("v"), 5)
            .setField(node.findFieldByName("next"), inner)
            .build();
    Validator validator = ValidatorFactory.newBuilder().build();
    assertThat(validator.validate(root).getViolations()).hasSize(2);
  }

  @Test
  public void testMessageWithoutRulesIsTautology() throws Exception {
    Map<String, Descriptor> types =
        compile(
            "recursive",
            DescriptorProto.newBuilder()
                .setName("Mid")
                .addField(messageField("leaf", 1, ".recursive.Leaf"))
                .addField(field("s", 2, FieldDescriptorProto.Type.TYPE_STRING)),
            DescriptorProto.newBuilder()
                .setName("Leaf")
                .addField(field("v", 1, FieldDescriptorProto.Type.TYPE_INT32)));
    assertThat(isTautology(types.get("Mid"))).isTrue();
  }

  @Test
  public void testRuleFreeCycleIsKept() throws Exception {
    Map<String, Descriptor> types =
        compile(
            "recursive",
            DescriptorProto.newBuilder()
                .setName("Root")
                .addField(
                    field("name", 1, FieldDescriptorProto.Type.TYPE_STRING)
                        .setOptions(
                            rules(
                                FieldRules.newBuilder()
                                    .setString(StringRules.newBuilder().setMinLen(1)))))
                .addField(messageField("node", 2, ".recursive.Node")),
            DescriptorProto.newBuilder()
                .setName("Node")
                .addField(messageField("next", 1, ".recursive.Node")));
    Descriptor root = types.get("Root");
    Descriptor node = types.get("Node");
    // Detecting that a cycle has no rules isn't worth the complexity, so it is walked.
    assertThat(isTautology(node)).isFalse();
    DynamicMessage message =
        DynamicMessage.newBuilder(root)
            .setField(root.findFieldByName("node"), DynamicMessage.getDefaultInstance(node))
            .build();
    Validator validator = ValidatorFactory.newBuilder().build();
    assertThat(validator.validate(message).getViolations()).hasSize(1);
  }

  @Test
  public void testStructTypesAreTautologies() throws Exception {
    assertThat(isTautology(Struct.getDescriptor())).isTrue();
    assertThat(isTautology(Value.getDescriptor())).isTrue();
    assertThat(isTautology(ListValue.getDescriptor())).isTrue();
  }

  @Test
  public void testNonStandardListValueWithRulesIsValidated() throws Exception {
    Map<String, Descriptor> types =
        compile(
            "google.protobuf",
            DescriptorProto.newBuilder()
                .setName("Value")
                .addField(field("string_value", 3, FieldDescriptorProto.Type.TYPE_STRING)),
            DescriptorProto.newBuilder()
                .setName("ListValue")
                .addField(
                    messageField("values", 1, ".google.protobuf.Value")
                        .setLabel(FieldDescriptorProto.Label.LABEL_REPEATED)
                        .setOptions(
                            rules(
                                FieldRules.newBuilder()
                                    .setRepeated(RepeatedRules.newBuilder().setMaxItems(1))))),
            DescriptorProto.newBuilder()
                .setName("Root")
                .addField(messageField("list", 1, ".google.protobuf.ListValue")));
    Descriptor value = types.get("Value");
    Descriptor listValue = types.get("ListValue");
    Descriptor root = types.get("Root");
    DynamicMessage item =
        DynamicMessage.newBuilder(value)
            .setField(value.findFieldByName("string_value"), "x")
            .build();
    DynamicMessage list =
        DynamicMessage.newBuilder(listValue)
            .addRepeatedField(listValue.findFieldByName("values"), item)
            .addRepeatedField(listValue.findFieldByName("values"), item)
            .build();
    DynamicMessage message =
        DynamicMessage.newBuilder(root).setField(root.findFieldByName("list"), list).build();
    Validator validator = ValidatorFactory.newBuilder().build();
    assertThat(validator.validate(message).getViolations()).hasSize(1);
  }

  @Test
  @Timeout(value = 15, unit = TimeUnit.SECONDS)
  public void testSharedChainBuildsInLinearTime() throws Exception {
    // Each M{i} has two required fields of type M{i+1}, so descending into shared evaluators
    // when checking for tautologies would do 2^n work.
    int n = 26;
    DescriptorProto.Builder[] messages = new DescriptorProto.Builder[n + 1];
    FieldOptions required = rules(FieldRules.newBuilder().setRequired(true));
    for (int i = 0; i < n; i++) {
      String next = ".recursive.M" + (i + 1);
      messages[i] =
          DescriptorProto.newBuilder()
              .setName("M" + i)
              .addField(messageField("a", 1, next).setOptions(required))
              .addField(messageField("b", 2, next).setOptions(required));
    }
    messages[n] = DescriptorProto.newBuilder().setName("M" + n);
    Descriptor m0 = compile("recursive", messages).get("M0");
    Validator validator = ValidatorFactory.newBuilder().build();
    assertThat(validator.validate(DynamicMessage.getDefaultInstance(m0)).getViolations())
        .hasSize(2);
  }

  private static Map<String, Descriptor> mutuallyRecursiveTypes() throws Exception {
    return compile(
        "recursive",
        DescriptorProto.newBuilder().setName("A").addField(messageField("b", 1, ".recursive.B")),
        DescriptorProto.newBuilder()
            .setName("B")
            .addField(messageField("a", 1, ".recursive.A"))
            .addField(
                field("v", 2, FieldDescriptorProto.Type.TYPE_INT32).setOptions(INT32_GT_100)));
  }

  /** Returns an A whose chain of B messages is {@code depth} long, each with v set to {@code v}. */
  private static DynamicMessage nestedA(Descriptor a, Descriptor b, int depth, int v) {
    DynamicMessage.Builder bBuilder =
        DynamicMessage.newBuilder(b).setField(b.findFieldByName("v"), v);
    if (depth > 1) {
      bBuilder.setField(b.findFieldByName("a"), nestedA(a, b, depth - 1, v));
    }
    return DynamicMessage.newBuilder(a).setField(a.findFieldByName("b"), bBuilder.build()).build();
  }

  private static boolean isTautology(Descriptor descriptor) throws Exception {
    EvaluatorBuilder builder =
        new EvaluatorBuilder(ValidateLibrary.newCel(), Config.newBuilder().build());
    return builder.load(descriptor).tautology();
  }

  private static FieldDescriptorProto.Builder field(
      String name, int number, FieldDescriptorProto.Type type) {
    return FieldDescriptorProto.newBuilder().setName(name).setNumber(number).setType(type);
  }

  private static FieldDescriptorProto.Builder messageField(
      String name, int number, String typeName) {
    return field(name, number, FieldDescriptorProto.Type.TYPE_MESSAGE).setTypeName(typeName);
  }

  private static FieldOptions rules(FieldRules.Builder rules) {
    return FieldOptions.newBuilder().setExtension(ValidateProto.field, rules.build()).build();
  }

  private static Map<String, Descriptor> compile(String pkg, DescriptorProto.Builder... messages)
      throws Exception {
    FileDescriptorProto.Builder file =
        FileDescriptorProto.newBuilder()
            .setName("recursive_test.proto")
            .setPackage(pkg)
            .setSyntax("proto3")
            .addDependency("buf/validate/validate.proto");
    for (DescriptorProto.Builder message : messages) {
      file.addMessageType(message);
    }
    FileDescriptor descriptor =
        FileDescriptor.buildFrom(
            file.build(), new FileDescriptor[] {ValidateProto.getDescriptor()});
    Map<String, Descriptor> types = new HashMap<>();
    for (Descriptor message : descriptor.getMessageTypes()) {
      types.put(message.getName(), message);
    }
    return types;
  }
}
