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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import build.buf.protovalidate.exceptions.CompilationException;
import build.buf.validate.FieldRules;
import build.buf.validate.RepeatedRules;
import build.buf.validate.Rule;
import build.buf.validate.ValidateProto;
import com.example.imports.validationtest.HasGoogleFields;
import com.example.imports.validationtest.NoRuleParent;
import com.example.imports.validationtest.Node;
import com.example.imports.validationtest.RecursiveA;
import com.example.imports.validationtest.RecursiveB;
import com.example.imports.validationtest.Root;
import com.example.imports.validationtest.SelfRecursive;
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
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Message evaluators are cached before they are built so recursive references can resolve to them.
 * These tests cover references made while the referenced evaluator is still incomplete.
 */
public class RecursiveMessageTest {
  @Test
  public void testMutuallyRecursiveMessagesValidateEveryDepth() throws Exception {
    Validator validator = ValidatorFactory.newBuilder().build();

    // depth 2
    List<Violation> violations = validator.validate(nestedA(2, 5)).getViolations();
    assertThat(violations).hasSize(2);
    assertThat(violations)
        .extracting(violation -> FieldPathUtils.fieldPathString(violation.toProto().getField()))
        .containsExactlyInAnyOrder("b.v", "b.a.b.v");
    for (Violation violation : violations) {
      assertThat(violation.getRuleValue().getDescriptor().getFullName())
          .isEqualTo("buf.validate.Int32Rules.gt");
      assertThat(violation.getRuleValue().getValue()).isEqualTo(100);
    }

    // depth 3 also
    List<Violation> violations1 = validator.validate(nestedA(3, 5)).getViolations();
    assertThat(violations1).hasSize(3);
    assertThat(violations1)
        .extracting(violation -> FieldPathUtils.fieldPathString(violation.toProto().getField()))
        .containsExactlyInAnyOrder("b.v", "b.a.b.v", "b.a.b.a.b.v");
    for (Violation violation : violations1) {
      assertThat(violation.getRuleValue().getDescriptor().getFullName())
          .isEqualTo("buf.validate.Int32Rules.gt");
      assertThat(violation.getRuleValue().getValue()).isEqualTo(100);
    }
  }

  @Test
  public void testMutuallyRecursiveMessagesValidateFromEitherRoot() throws Exception {
    RecursiveB recursiveB = RecursiveB.newBuilder().setV(5).setA(nestedA(1, 5)).build();
    Validator validator = ValidatorFactory.newBuilder().build();
    List<Violation> violations = validator.validate(recursiveB).getViolations();
    assertThat(violations).hasSize(2);
    assertThat(violations)
        .extracting(violation -> FieldPathUtils.fieldPathString(violation.toProto().getField()))
        .containsExactlyInAnyOrder("v", "a.b.v");
    for (Violation violation : violations) {
      assertThat(violation.getRuleValue().getDescriptor().getFullName())
          .isEqualTo("buf.validate.Int32Rules.gt");
      assertThat(violation.getRuleValue().getValue()).isEqualTo(100);
    }
  }

  @Test
  public void testMutuallyRecursiveMessagesAcceptValidValues() throws Exception {
    Validator validator = ValidatorFactory.newBuilder().build();
    assertThat(validator.validate(nestedA(2, 101)).isSuccess()).isTrue();
  }

  @Test
  public void testSelfRecursiveFieldDeclaredBeforeRule() throws Exception {
    SelfRecursive selfRecursive =
        SelfRecursive.newBuilder()
            .setV(5)
            .setNext(SelfRecursive.newBuilder().setV(5).build())
            .build();
    Validator validator = ValidatorFactory.newBuilder().build();
    List<Violation> violations = validator.validate(selfRecursive).getViolations();
    assertThat(violations).hasSize(2);
    assertThat(violations)
        .extracting(violation -> FieldPathUtils.fieldPathString(violation.toProto().getField()))
        .containsExactlyInAnyOrder("v", "next.v");
    for (Violation violation : violations) {
      assertThat(violation.getRuleValue().getDescriptor().getFullName())
          .isEqualTo("buf.validate.Int32Rules.gt");
      assertThat(violation.getRuleValue().getValue()).isEqualTo(100);
    }
  }

  @Test
  public void testMessageWithoutRulesIsTautology() throws Exception {
    assertThat(isTautology(NoRuleParent.getDescriptor())).isTrue();
  }

  @Test
  public void testRuleFreeCycleIsKept() throws Exception {
    // Detecting that a cycle has no rules isn't worth the complexity, so it is walked.
    // node has a single field that's itself, but it has no rules
    assertThat(isTautology(Node.getDescriptor())).isFalse();
    // root has a single rule on its name field, which must have a length > 1
    Root root = Root.newBuilder().setNode(Node.newBuilder().build()).build();
    Validator validator = ValidatorFactory.newBuilder().build();
    List<Violation> violations = validator.validate(root).getViolations();
    assertThat(violations).hasSize(1);
    Violation violation = violations.get(0);
    assertThat(FieldPathUtils.fieldPathString(violation.toProto().getField())).isEqualTo("name");
    assertThat(violation.getRuleValue().getDescriptor().getFullName())
        .isEqualTo("buf.validate.StringRules.min_len");
    assertThat(violation.getRuleValue().getValue()).isEqualTo(1L);
  }

  @Test
  public void testStructTypesAreTautologies() throws Exception {
    assertThat(isTautology(Struct.getDescriptor())).isTrue();
    assertThat(isTautology(Value.getDescriptor())).isTrue();
    assertThat(isTautology(ListValue.getDescriptor())).isTrue();
    assertThat(isTautology(HasGoogleFields.getDescriptor())).isTrue();
  }

  @Test
  public void testNonStandardListValueWithRulesIsValidated() throws Exception {
    // these are left dynamic intentionally
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
  public void testCompilationErrorReportedWhenInnerIsBuiltFirst() throws Exception {
    Map<String, Descriptor> types = compilationErrorTypes();
    Validator validator = ValidatorFactory.newBuilder().build();
    assertThatThrownBy(() -> validator.validate(innerWithOuter(types)))
        .isInstanceOf(CompilationException.class);
  }

  @Test
  public void testCompilationErrorReportedForInnerAfterOuterFailed() throws Exception {
    Map<String, Descriptor> types = compilationErrorTypes();
    Validator validator = ValidatorFactory.newBuilder().build();
    DynamicMessage outer = DynamicMessage.getDefaultInstance(types.get("Outer"));
    assertThatThrownBy(() -> validator.validate(outer)).isInstanceOf(CompilationException.class);
    assertThatThrownBy(() -> validator.validate(innerWithOuter(types)))
        .isInstanceOf(CompilationException.class);
  }

  @Test
  public void testCompilationErrorReportedForOuterOnEveryValidation() throws Exception {
    Map<String, Descriptor> types = compilationErrorTypes();
    Validator validator = ValidatorFactory.newBuilder().build();
    DynamicMessage outer = DynamicMessage.getDefaultInstance(types.get("Outer"));
    for (int i = 0; i < 2; i++) {
      assertThatThrownBy(() -> validator.validate(outer)).isInstanceOf(CompilationException.class);
    }
  }

  /**
   * Outer's invalid rule is on a field after its Inner field, so building Outer fails after Inner
   * has been built against the incomplete Outer evaluator.
   */
  private static Map<String, Descriptor> compilationErrorTypes() throws Exception {
    return compile(
        "recursive",
        DescriptorProto.newBuilder()
            .setName("Outer")
            .addField(messageField("inner", 1, ".recursive.Inner"))
            .addField(
                field("s", 2, FieldDescriptorProto.Type.TYPE_STRING)
                    .setOptions(
                        rules(
                            FieldRules.newBuilder()
                                .addCel(Rule.newBuilder().setId("bad").setExpression("this +"))))),
        DescriptorProto.newBuilder()
            .setName("Inner")
            .addField(
                messageField("outer", 1, ".recursive.Outer")
                    .setOptions(rules(FieldRules.newBuilder().setRequired(true)))));
  }

  private static DynamicMessage innerWithOuter(Map<String, Descriptor> types) {
    Descriptor inner = types.get("Inner");
    return DynamicMessage.newBuilder(inner)
        .setField(
            inner.findFieldByName("outer"), DynamicMessage.getDefaultInstance(types.get("Outer")))
        .build();
  }

  /** Returns an A whose chain of B messages is {@code depth} long, each with v set to {@code v}. */
  private static RecursiveA nestedA(int depth, int v) {
    RecursiveA recursiveA = null;
    RecursiveB recursiveB = null;
    for (int i = 0; i < depth; i++) {
      RecursiveB.Builder recursiveBBuilder = RecursiveB.newBuilder().setV(v);
      if (recursiveA != null) {
        recursiveBBuilder.setA(recursiveA);
      }
      recursiveB = recursiveBBuilder.build();
      recursiveA = RecursiveA.newBuilder().setB(recursiveB).build();
    }
    return recursiveA;
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
