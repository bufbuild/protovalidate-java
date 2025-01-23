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

import static com.example.imports.validationtest.PredefinedProto.isIdent;
import static org.assertj.core.api.Assertions.assertThat;

import build.buf.validate.FieldConstraints;
import build.buf.validate.FieldPath;
import build.buf.validate.FieldPathElement;
import build.buf.validate.Violation;
import com.example.imports.buf.validate.StringRules;
import com.example.imports.validationtest.ExamplePredefinedFieldConstraints;
import com.example.noimports.validationtest.ExampleFieldConstraints;
import com.example.noimports.validationtest.ExampleMessageConstraints;
import com.example.noimports.validationtest.ExampleOneofConstraints;
import com.example.noimports.validationtest.ExampleRequiredFieldConstraints;
import com.example.noimports.validationtest.PredefinedProto;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.TypeRegistry;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * This test mimics the behavior when performing validation with protovalidate on a file descriptor
 * set (as created by <code>protoc --retain_options --descriptor_set_out=...</code>). These
 * descriptor types have the protovalidate extensions as unknown fields and need to be parsed with
 * an extension registry for the constraints to be recognized and validated.
 */
public class ValidatorDynamicMessageTest {

  @Test
  public void testFieldConstraintDynamicMessage() throws Exception {
    DynamicMessage.Builder messageBuilder =
        createMessageWithUnknownOptions(ExampleFieldConstraints.getDefaultInstance());
    messageBuilder.setField(
        messageBuilder.getDescriptorForType().findFieldByName("regex_string_field"), "0123456789");
    Violation expectedViolation =
        Violation.newBuilder()
            .setField(
                FieldPath.newBuilder()
                    .addElements(
                        FieldPathUtils.fieldPathElement(
                            messageBuilder
                                .getDescriptorForType()
                                .findFieldByName("regex_string_field"))))
            .setRule(
                FieldPath.newBuilder()
                    .addElements(
                        FieldPathUtils.fieldPathElement(
                            FieldConstraints.getDescriptor()
                                .findFieldByNumber(FieldConstraints.STRING_FIELD_NUMBER)))
                    .addElements(
                        FieldPathUtils.fieldPathElement(
                            StringRules.getDescriptor()
                                .findFieldByNumber(StringRules.PATTERN_FIELD_NUMBER))))
            .setConstraintId("string.pattern")
            .setFieldPath("regex_string_field")
            .setMessage("value does not match regex pattern `^[a-z0-9]{1,9}$`")
            .build();
    ValidationResult result = new Validator().validate(messageBuilder.build());
    assertThat(result.toProto().getViolationsList()).containsExactly(expectedViolation);
    assertThat(result.getViolations().get(0).getFieldValue().getValue()).isEqualTo("0123456789");
    assertThat(result.getViolations().get(0).getRuleValue().getValue())
        .isEqualTo("^[a-z0-9]{1,9}$");
  }

  @Test
  public void testOneofConstraintDynamicMessage() throws Exception {
    DynamicMessage.Builder messageBuilder =
        createMessageWithUnknownOptions(ExampleOneofConstraints.getDefaultInstance());
    Violation expectedViolation =
        Violation.newBuilder()
            .setField(
                FieldPath.newBuilder()
                    .addElements(FieldPathElement.newBuilder().setFieldName("contact_info")))
            .setFieldPath("contact_info")
            .setConstraintId("required")
            .setMessage("exactly one field is required in oneof")
            .build();
    assertThat(new Validator().validate(messageBuilder.build()).toProto().getViolationsList())
        .containsExactly(expectedViolation);
  }

  @Test
  public void testMessageConstraintDynamicMessage() throws Exception {
    DynamicMessage.Builder messageBuilder =
        createMessageWithUnknownOptions(ExampleMessageConstraints.getDefaultInstance());
    messageBuilder.setField(
        messageBuilder.getDescriptorForType().findFieldByName("secondary_email"),
        "something@somewhere.com");
    Violation expectedViolation =
        Violation.newBuilder()
            .setConstraintId("secondary_email_depends_on_primary")
            .setMessage("cannot set a secondary email without setting a primary one")
            .build();
    assertThat(new Validator().validate(messageBuilder.build()).toProto().getViolationsList())
        .containsExactly(expectedViolation);
  }

  @Test
  public void testRequiredFieldConstraintDynamicMessage() throws Exception {
    DynamicMessage.Builder messageBuilder =
        createMessageWithUnknownOptions(ExampleRequiredFieldConstraints.getDefaultInstance());
    messageBuilder.setField(
        messageBuilder.getDescriptorForType().findFieldByName("regex_string_field"), "abc123");
    assertThat(new Validator().validate(messageBuilder.build()).getViolations()).isEmpty();
  }

  @Test
  public void testRequiredFieldConstraintDynamicMessageInvalid() throws Exception {
    DynamicMessage.Builder messageBuilder =
        createMessageWithUnknownOptions(ExampleRequiredFieldConstraints.getDefaultInstance());
    messageBuilder.setField(
        messageBuilder.getDescriptorForType().findFieldByName("regex_string_field"), "0123456789");
    Violation expectedViolation =
        Violation.newBuilder()
            .setField(
                FieldPath.newBuilder()
                    .addElements(
                        FieldPathUtils.fieldPathElement(
                            messageBuilder
                                .getDescriptorForType()
                                .findFieldByName("regex_string_field"))))
            .setRule(
                FieldPath.newBuilder()
                    .addElements(
                        FieldPathUtils.fieldPathElement(
                            FieldConstraints.getDescriptor()
                                .findFieldByNumber(FieldConstraints.STRING_FIELD_NUMBER)))
                    .addElements(
                        FieldPathUtils.fieldPathElement(
                            StringRules.getDescriptor()
                                .findFieldByNumber(StringRules.PATTERN_FIELD_NUMBER))))
            .setConstraintId("string.pattern")
            .setFieldPath("regex_string_field")
            .setMessage("value does not match regex pattern `^[a-z0-9]{1,9}$`")
            .build();
    assertThat(new Validator().validate(messageBuilder.build()).toProto().getViolationsList())
        .containsExactly(expectedViolation);
  }

  @Test
  public void testPredefinedFieldConstraintDynamicMessage() throws Exception {
    DynamicMessage.Builder messageBuilder =
        createMessageWithUnknownOptions(ExamplePredefinedFieldConstraints.getDefaultInstance());
    messageBuilder.setField(
        messageBuilder.getDescriptorForType().findFieldByName("ident_field"), "abc123");
    ExtensionRegistry registry = ExtensionRegistry.newInstance();
    registry.add(isIdent);
    TypeRegistry typeRegistry =
        TypeRegistry.newBuilder().add(isIdent.getDescriptor().getContainingType()).build();
    Config config =
        Config.newBuilder().setExtensionRegistry(registry).setTypeRegistry(typeRegistry).build();
    assertThat(new Validator(config).validate(messageBuilder.build()).getViolations()).isEmpty();
  }

  @Test
  public void testPredefinedFieldConstraintDynamicMessageInvalid() throws Exception {
    DynamicMessage.Builder messageBuilder =
        createMessageWithUnknownOptions(ExamplePredefinedFieldConstraints.getDefaultInstance());
    messageBuilder.setField(
        messageBuilder.getDescriptorForType().findFieldByName("ident_field"), "0123456789");
    Violation expectedViolation =
        Violation.newBuilder()
            .setField(
                FieldPath.newBuilder()
                    .addElements(
                        FieldPathUtils.fieldPathElement(
                            messageBuilder.getDescriptorForType().findFieldByName("ident_field"))))
            .setRule(
                FieldPath.newBuilder()
                    .addElements(
                        FieldPathUtils.fieldPathElement(
                            FieldConstraints.getDescriptor()
                                .findFieldByNumber(FieldConstraints.STRING_FIELD_NUMBER)))
                    .addElements(
                        FieldPathUtils.fieldPathElement(PredefinedProto.isIdent.getDescriptor())))
            .setConstraintId("string.is_ident")
            .setFieldPath("ident_field")
            .setMessage("invalid identifier")
            .build();
    ExtensionRegistry registry = ExtensionRegistry.newInstance();
    registry.add(isIdent);
    TypeRegistry typeRegistry =
        TypeRegistry.newBuilder().add(isIdent.getDescriptor().getContainingType()).build();
    Config config =
        Config.newBuilder().setExtensionRegistry(registry).setTypeRegistry(typeRegistry).build();
    assertThat(new Validator(config).validate(messageBuilder.build()).toProto().getViolationsList())
        .containsExactly(expectedViolation);
  }

  private static void gatherDependencies(
      Descriptors.FileDescriptor fd, Set<DescriptorProtos.FileDescriptorProto> dependencies) {
    dependencies.add(fd.toProto());
    for (Descriptors.FileDescriptor dependency : fd.getDependencies()) {
      gatherDependencies(dependency, dependencies);
    }
  }

  private static DescriptorProtos.FileDescriptorSet createFileDescriptorSetForMessage(
      Descriptors.Descriptor message) {
    DescriptorProtos.FileDescriptorSet.Builder builder =
        DescriptorProtos.FileDescriptorSet.newBuilder();
    Set<DescriptorProtos.FileDescriptorProto> dependencies = new LinkedHashSet<>();
    gatherDependencies(message.getFile(), dependencies);
    builder.addAllFile(dependencies);
    return builder.build();
  }

  private static Descriptors.FileDescriptor getFileDescriptor(
      String name, Map<String, DescriptorProtos.FileDescriptorProto> fds)
      throws Descriptors.DescriptorValidationException {
    DescriptorProtos.FileDescriptorProto fdProto = fds.get(name);
    if (fdProto == null) {
      throw new IllegalArgumentException("unable to file file descriptor proto: " + name);
    }
    Descriptors.FileDescriptor[] dependencies =
        new Descriptors.FileDescriptor[fdProto.getDependencyCount()];
    for (int i = 0; i < fdProto.getDependencyCount(); i++) {
      dependencies[i] = getFileDescriptor(fdProto.getDependency(i), fds);
    }
    return Descriptors.FileDescriptor.buildFrom(fdProto, dependencies);
  }

  private static DynamicMessage.Builder createMessageWithUnknownOptions(Message message)
      throws InvalidProtocolBufferException, Descriptors.DescriptorValidationException {
    DescriptorProtos.FileDescriptorSet fds =
        createFileDescriptorSetForMessage(message.getDescriptorForType());
    // Reparse file descriptor set from encoded form (loses known extensions).
    fds = DescriptorProtos.FileDescriptorSet.parseFrom(fds.toByteArray());
    Map<String, DescriptorProtos.FileDescriptorProto> fdsMap =
        fds.getFileList().stream()
            .collect(
                Collectors.toMap(
                    DescriptorProtos.FileDescriptorProto::getName, Function.identity()));
    Descriptors.FileDescriptor descriptor =
        getFileDescriptor(message.getDescriptorForType().getFile().getName(), fdsMap);
    return DynamicMessage.newBuilder(
        descriptor.findMessageTypeByName(message.getDescriptorForType().getName()));
  }
}
