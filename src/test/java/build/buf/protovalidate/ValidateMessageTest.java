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

import build.buf.validate.FieldPathElement;
import build.buf.validate.FieldRules;
import build.buf.validate.Int32Rules;
import build.buf.validate.MapRules;
import build.buf.validate.MessageRules;
import build.buf.validate.RepeatedRules;
import build.buf.validate.Rule;
import build.buf.validate.StringRules;
import build.buf.validate.TimestampRules;
import build.buf.validate.ValidateProto;
import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldOptions;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.MessageOptions;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * Exercises {@link Validator#validate(ValidateMessage)} with an alternative-runtime {@link
 * ValidateMessage} backed by a plain map of field values, so the interface is covered without a
 * {@code com.google.protobuf.Message}.
 */
public class ValidateMessageTest {
  @Test
  public void violations() throws Exception {
    // "Person" has two scalar fields with buf.validate.field rules and a
    // message-level buf.validate.message CEL rule.
    Descriptors.Descriptor descriptor = buildPersonDescriptor();

    // name "ab" violates the field rule (min_len = 3); age 200 satisfies the field
    // rule (gte = 0) but violates the message-level CEL rule (this.age <= 150).
    Descriptors.Descriptor addressDescriptor =
        descriptor.findFieldByName("address").getMessageType();
    ValidateMessage message =
        person(descriptor, "ab", 200, address(addressDescriptor, "Main Street"));
    Validator validator = ValidatorFactory.newBuilder().build();
    ValidationResult result = validator.validate(message);

    assertThat(result.getViolations()).hasSize(2);
    assertThat(result.getViolations())
        .extracting(violation -> violation.toProto().getRuleId())
        .containsExactlyInAnyOrder("string.min_len", "person.age_range");
  }

  @Test
  public void nestedViolation() throws Exception {
    Descriptors.Descriptor descriptor = buildPersonDescriptor();

    // Person is valid, but the nested Address.street violates its own min_len rule.
    Descriptors.Descriptor addressDescriptor =
        descriptor.findFieldByName("address").getMessageType();
    ValidateMessage message = person(descriptor, "alice", 30, address(addressDescriptor, "x"));
    Validator validator = ValidatorFactory.newBuilder().build();
    ValidationResult result = validator.validate(message);

    assertThat(result.getViolations()).hasSize(1);
    assertThat(result.getViolations().get(0).toProto().getRuleId()).isEqualTo("string.min_len");
    // The violation path is reported relative to the root message: address.street.
    List<String> fieldPath =
        result.getViolations().get(0).toProto().getField().getElementsList().stream()
            .map(FieldPathElement::getFieldName)
            .collect(Collectors.toList());
    assertThat(fieldPath).containsExactly("address", "street");
  }

  @Test
  public void valid() throws Exception {
    Descriptors.Descriptor descriptor = buildPersonDescriptor();

    // Satisfies every rule: name long enough, age in [0, 150], street long enough.
    Descriptors.Descriptor addressDescriptor =
        descriptor.findFieldByName("address").getMessageType();
    ValidateMessage message =
        person(descriptor, "alice", 30, address(addressDescriptor, "Main Street"));
    Validator validator = ValidatorFactory.newBuilder().build();
    ValidationResult result = validator.validate(message);

    assertThat(result.getViolations()).isEmpty();
  }

  @Test
  public void repeatedViolation() throws Exception {
    Descriptors.Descriptor descriptor = buildPersonDescriptor();
    Descriptors.Descriptor addressDescriptor =
        descriptor.findFieldByName("address").getMessageType();

    // nicknames = ["jo", "x"]; the second element "x" violates the items min_len = 2.
    Map<Descriptors.FieldDescriptor, Object> fields =
        personFields(descriptor, "alice", 30, address(addressDescriptor, "Main Street"));
    fields.put(descriptor.findFieldByName("nicknames"), Arrays.asList("jo", "x"));
    ValidateMessage message = new MapValidateMessage(descriptor, fields);

    ValidationResult result = ValidatorFactory.newBuilder().build().validate(message);

    assertThat(result.getViolations()).hasSize(1);
    assertThat(result.getViolations().get(0).toProto().getRuleId()).isEqualTo("string.min_len");
    // The violation path points at the offending element: nicknames[1].
    FieldPathElement element = result.getViolations().get(0).toProto().getField().getElements(0);
    assertThat(element.getFieldName()).isEqualTo("nicknames");
    assertThat(element.getIndex()).isEqualTo(1);
  }

  @Test
  public void mapViolation() throws Exception {
    Descriptors.Descriptor descriptor = buildPersonDescriptor();
    Descriptors.Descriptor addressDescriptor =
        descriptor.findFieldByName("address").getMessageType();

    // scores = {"math": 90, "art": -5}; the "art" value violates values gte = 0.
    Map<String, Integer> scores = new LinkedHashMap<>();
    scores.put("math", 90);
    scores.put("art", -5);
    Map<Descriptors.FieldDescriptor, Object> fields =
        personFields(descriptor, "alice", 30, address(addressDescriptor, "Main Street"));
    fields.put(descriptor.findFieldByName("scores"), scores);
    ValidateMessage message = new MapValidateMessage(descriptor, fields);

    ValidationResult result = ValidatorFactory.newBuilder().build().validate(message);

    assertThat(result.getViolations()).hasSize(1);
    assertThat(result.getViolations().get(0).toProto().getRuleId()).isEqualTo("int32.gte");
    // The violation path points at the offending entry: scores["art"].
    FieldPathElement element = result.getViolations().get(0).toProto().getField().getElements(0);
    assertThat(element.getFieldName()).isEqualTo("scores");
    assertThat(element.getStringKey()).isEqualTo("art");
  }

  @Test
  public void repeatedMessageViolation() throws Exception {
    Descriptors.Descriptor descriptor = buildPersonDescriptor();
    Descriptors.Descriptor addressDescriptor =
        descriptor.findFieldByName("address").getMessageType();

    // previous_addresses = [Address("Main Street"), Address("x")]; the second element's
    // street violates min_len = 4, reached through a repeated message element.
    Map<Descriptors.FieldDescriptor, Object> fields =
        personFields(descriptor, "alice", 30, address(addressDescriptor, "Main Street"));
    fields.put(
        descriptor.findFieldByName("previous_addresses"),
        Arrays.asList(address(addressDescriptor, "Main Street"), address(addressDescriptor, "x")));
    ValidateMessage message = new MapValidateMessage(descriptor, fields);

    ValidationResult result = ValidatorFactory.newBuilder().build().validate(message);

    assertThat(result.getViolations()).hasSize(1);
    assertThat(result.getViolations().get(0).toProto().getRuleId()).isEqualTo("string.min_len");
    List<FieldPathElement> path =
        result.getViolations().get(0).toProto().getField().getElementsList();
    assertThat(path)
        .extracting(FieldPathElement::getFieldName)
        .containsExactly("previous_addresses", "street");
    // The element index is carried on the repeated field path element.
    assertThat(path.get(0).getIndex()).isEqualTo(1);
  }

  @Test
  public void mapMessageViolation() throws Exception {
    Descriptors.Descriptor descriptor = buildPersonDescriptor();
    Descriptors.Descriptor addressDescriptor =
        descriptor.findFieldByName("address").getMessageType();

    // homes = {"primary": Address("x")}; the value message's street violates min_len = 4,
    // reached through a map value message.
    Map<String, ValidateMessage> homes = new LinkedHashMap<>();
    homes.put("primary", address(addressDescriptor, "x"));
    Map<Descriptors.FieldDescriptor, Object> fields =
        personFields(descriptor, "alice", 30, address(addressDescriptor, "Main Street"));
    fields.put(descriptor.findFieldByName("homes"), homes);
    ValidateMessage message = new MapValidateMessage(descriptor, fields);

    ValidationResult result = ValidatorFactory.newBuilder().build().validate(message);

    assertThat(result.getViolations()).hasSize(1);
    assertThat(result.getViolations().get(0).toProto().getRuleId()).isEqualTo("string.min_len");
    List<FieldPathElement> path =
        result.getViolations().get(0).toProto().getField().getElementsList();
    assertThat(path).extracting(FieldPathElement::getFieldName).containsExactly("homes", "street");
    // The map key is carried on the map field path element.
    assertThat(path.get(0).getStringKey()).isEqualTo("primary");
  }

  @Test
  public void messageFieldRequiresValidateMessage() throws Exception {
    Descriptors.Descriptor descriptor = buildPersonDescriptor();
    Descriptors.Descriptor addressDescriptor =
        descriptor.findFieldByName("address").getMessageType();

    // A singular message field stored as a non-ValidateMessage value is rejected with a clear
    // error, rather than failing later as an opaque ClassCastException.
    Map<Descriptors.FieldDescriptor, Object> fields =
        personFields(descriptor, "alice", 30, address(addressDescriptor, "Main Street"));
    fields.put(descriptor.findFieldByName("address"), "not a ValidateMessage");
    ValidateMessage message = new MapValidateMessage(descriptor, fields);

    Validator validator = ValidatorFactory.newBuilder().build();
    assertThatThrownBy(() -> validator.validate(message))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("address")
        .hasMessageContaining("ValidateMessage");
  }

  @Test
  public void messageValueJvmValueUnsupported() throws Exception {
    // A message has no raw scalar form, so jvmValue is unsupported on a message-backed Value.
    Descriptors.Descriptor descriptor = buildPersonDescriptor();
    Descriptors.Descriptor addressDescriptor =
        descriptor.findFieldByName("address").getMessageType();

    Value messageValue = new ValidateMessageValue(address(addressDescriptor, "Main Street"));
    assertThatThrownBy(() -> messageValue.jvmValue(Object.class))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessageContaining("jvmValue returns a raw scalar for native rule evaluation")
        .hasMessageContaining("not supported on a message value")
        .hasMessageContaining("ValidateMessage.getField() implementation returned a message value");

    Value protobufMessageValue = new MessageValue(Timestamp.getDefaultInstance());
    assertThatThrownBy(() -> protobufMessageValue.jvmValue(Object.class))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessageContaining("jvmValue returns a raw scalar for native rule evaluation")
        .hasMessageContaining("not supported on a message value")
        .hasMessageContaining("use messageValue() or celValue() instead");
  }

  @Test
  public void wellKnownTypeTimestamp() throws Exception {
    Descriptors.Descriptor descriptor = buildEventDescriptor();
    Validator validator = ValidatorFactory.newBuilder().build();

    // created_at is exposed as a ValidateMessage over google.protobuf.Timestamp, whose celValue()
    // surfaces a java.time.Instant. The standard timestamp.lt rule ("this >= rules.lt") can now
    // evaluate against it. rules.lt is seconds=1_000_000_000.

    // After the lt bound -> violation.
    Map<Descriptors.FieldDescriptor, Object> late = new HashMap<>();
    late.put(
        descriptor.findFieldByName("created_at"), timestampMessage(descriptor, 2_000_000_000L));
    ValidationResult lateResult = validator.validate(new MapValidateMessage(descriptor, late));
    assertThat(lateResult.getViolations()).hasSize(1);
    assertThat(lateResult.getViolations().get(0).toProto().getRuleId()).isEqualTo("timestamp.lt");

    // Before the lt bound -> valid.
    Map<Descriptors.FieldDescriptor, Object> early = new HashMap<>();
    early.put(descriptor.findFieldByName("created_at"), timestampMessage(descriptor, 0L));
    ValidationResult earlyResult = validator.validate(new MapValidateMessage(descriptor, early));
    assertThat(earlyResult.getViolations()).isEmpty();
  }

  @Test
  public void bytesViolation() throws Exception {
    Descriptors.Descriptor descriptor = buildScalarsDescriptor();

    // data = two bytes; violates the field rule (bytes min_len = 3).
    Map<Descriptors.FieldDescriptor, Object> fields = new HashMap<>();
    fields.put(
        descriptor.findFieldByName("data"),
        com.google.protobuf.ByteString.copyFrom(new byte[] {1, 2}));
    ValidationResult result =
        ValidatorFactory.newBuilder().build().validate(new MapValidateMessage(descriptor, fields));

    assertThat(result.getViolations()).hasSize(1);
    assertThat(result.getViolations().get(0).toProto().getRuleId()).isEqualTo("bytes.min_len");
  }

  @Test
  public void enumViolation() throws Exception {
    Descriptors.Descriptor descriptor = buildScalarsDescriptor();

    // color = RED (0); violates the field rule (enum const = GREEN).
    Map<Descriptors.FieldDescriptor, Object> fields = new HashMap<>();
    Descriptors.EnumDescriptor color = descriptor.findFieldByName("color").getEnumType();
    fields.put(descriptor.findFieldByName("color"), color.findValueByNumber(0));
    ValidationResult result =
        ValidatorFactory.newBuilder().build().validate(new MapValidateMessage(descriptor, fields));

    assertThat(result.getViolations()).hasSize(1);
    assertThat(result.getViolations().get(0).toProto().getRuleId()).isEqualTo("enum.const");
  }

  @Test
  public void unsignedViolation() throws Exception {
    Descriptors.Descriptor descriptor = buildScalarsDescriptor();

    // count = 5; violates the field rule (uint32 gt = 10).
    Map<Descriptors.FieldDescriptor, Object> fields = new HashMap<>();
    fields.put(descriptor.findFieldByName("count"), 5);
    ValidationResult result =
        ValidatorFactory.newBuilder().build().validate(new MapValidateMessage(descriptor, fields));

    assertThat(result.getViolations()).hasSize(1);
    assertThat(result.getViolations().get(0).toProto().getRuleId()).isEqualTo("uint32.gt");
  }

  @Test
  public void wrapperViolation() throws Exception {
    Descriptors.Descriptor descriptor = buildScalarsDescriptor();

    // wrapped = Int32Value(0); the inner value violates the field rule (int32 gt = 0).
    Descriptors.Descriptor wrapperDescriptor =
        descriptor.findFieldByName("wrapped").getMessageType();
    Map<Descriptors.FieldDescriptor, Object> wrapperFields = new HashMap<>();
    wrapperFields.put(wrapperDescriptor.findFieldByName("value"), 0);
    Map<Descriptors.FieldDescriptor, Object> fields = new HashMap<>();
    fields.put(
        descriptor.findFieldByName("wrapped"),
        new MapValidateMessage(wrapperDescriptor, wrapperFields));
    ValidationResult result =
        ValidatorFactory.newBuilder().build().validate(new MapValidateMessage(descriptor, fields));

    assertThat(result.getViolations()).hasSize(1);
    assertThat(result.getViolations().get(0).toProto().getRuleId()).isEqualTo("int32.gt");
  }

  @Test
  public void oneofRequiredViolation() throws Exception {
    Descriptors.Descriptor descriptor = buildOneofDescriptor();

    // The "choice" oneof is required, but no member is set.
    ValidationResult result =
        ValidatorFactory.newBuilder()
            .build()
            .validate(new MapValidateMessage(descriptor, new HashMap<>()));

    assertThat(result.getViolations()).hasSize(1);
    assertThat(result.getViolations().get(0).toProto().getRuleId()).isEqualTo("required");
    assertThat(result.getViolations().get(0).toProto().getField().getElements(0).getFieldName())
        .isEqualTo("choice");
  }

  /** Builds a {@link MapValidateMessage} for the "Person" message type. */
  private static MapValidateMessage person(
      Descriptors.Descriptor descriptor, String name, int age, ValidateMessage address) {
    return new MapValidateMessage(descriptor, personFields(descriptor, name, age, address));
  }

  /** Builds the field-value map for a "Person", which tests can extend with optional fields. */
  private static Map<Descriptors.FieldDescriptor, Object> personFields(
      Descriptors.Descriptor descriptor, String name, int age, ValidateMessage address) {
    Map<Descriptors.FieldDescriptor, Object> fields = new HashMap<>();
    fields.put(descriptor.findFieldByName("name"), name);
    fields.put(descriptor.findFieldByName("age"), age);
    fields.put(descriptor.findFieldByName("address"), address);
    return fields;
  }

  /** Builds a {@link MapValidateMessage} for the "Address" message type. */
  private static MapValidateMessage address(
      Descriptors.Descriptor addressDescriptor, String street) {
    Map<Descriptors.FieldDescriptor, Object> fields = new HashMap<>();
    fields.put(addressDescriptor.findFieldByName("street"), street);
    return new MapValidateMessage(addressDescriptor, fields);
  }

  /**
   * Builds the "Person" {@link Descriptors.Descriptor} at runtime by assembling a {@link
   * FileDescriptorProto}. Field rules are attached via the {@code buf.validate.field} extension, a
   * message-level CEL rule via {@code buf.validate.message}, and a nested {@code Address} message
   * carries its own field rule.
   */
  private static Descriptors.Descriptor buildPersonDescriptor() throws Exception {
    DescriptorProto address =
        DescriptorProto.newBuilder()
            .setName("Address")
            .addField(
                FieldDescriptorProto.newBuilder()
                    .setName("street")
                    .setNumber(1)
                    .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL)
                    .setType(FieldDescriptorProto.Type.TYPE_STRING)
                    .setOptions(stringMinLen(4)))
            .build();

    DescriptorProto person =
        DescriptorProto.newBuilder()
            .setName("Person")
            .setOptions(
                MessageOptions.newBuilder()
                    .setExtension(
                        ValidateProto.message,
                        MessageRules.newBuilder()
                            .addCel(
                                Rule.newBuilder()
                                    .setId("person.age_range")
                                    .setMessage("age must be at most 150")
                                    .setExpression("this.age <= 150"))
                            .build()))
            .addField(
                FieldDescriptorProto.newBuilder()
                    .setName("name")
                    .setNumber(1)
                    .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL)
                    .setType(FieldDescriptorProto.Type.TYPE_STRING)
                    .setOptions(stringMinLen(3)))
            .addField(
                FieldDescriptorProto.newBuilder()
                    .setName("age")
                    .setNumber(2)
                    .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL)
                    .setType(FieldDescriptorProto.Type.TYPE_INT32)
                    .setOptions(
                        FieldOptions.newBuilder()
                            .setExtension(
                                ValidateProto.field,
                                FieldRules.newBuilder()
                                    .setInt32(Int32Rules.newBuilder().setGte(0))
                                    .build())))
            .addField(
                FieldDescriptorProto.newBuilder()
                    .setName("address")
                    .setNumber(3)
                    .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL)
                    .setType(FieldDescriptorProto.Type.TYPE_MESSAGE)
                    .setTypeName(".example.Address"))
            // repeated string nicknames, where each element must have min_len 2.
            .addField(
                FieldDescriptorProto.newBuilder()
                    .setName("nicknames")
                    .setNumber(4)
                    .setLabel(FieldDescriptorProto.Label.LABEL_REPEATED)
                    .setType(FieldDescriptorProto.Type.TYPE_STRING)
                    .setOptions(
                        fieldRule(
                            FieldRules.newBuilder()
                                .setRepeated(
                                    RepeatedRules.newBuilder()
                                        .setItems(
                                            FieldRules.newBuilder()
                                                .setString(
                                                    StringRules.newBuilder().setMinLen(2)))))))
            // map<string, int32> scores, where each value must be gte 0.
            .addField(
                FieldDescriptorProto.newBuilder()
                    .setName("scores")
                    .setNumber(5)
                    .setLabel(FieldDescriptorProto.Label.LABEL_REPEATED)
                    .setType(FieldDescriptorProto.Type.TYPE_MESSAGE)
                    .setTypeName(".example.Person.ScoresEntry")
                    .setOptions(
                        fieldRule(
                            FieldRules.newBuilder()
                                .setMap(
                                    MapRules.newBuilder()
                                        .setValues(
                                            FieldRules.newBuilder()
                                                .setInt32(Int32Rules.newBuilder().setGte(0)))))))
            // repeated Address previous_addresses; each element is recursively validated.
            .addField(
                FieldDescriptorProto.newBuilder()
                    .setName("previous_addresses")
                    .setNumber(6)
                    .setLabel(FieldDescriptorProto.Label.LABEL_REPEATED)
                    .setType(FieldDescriptorProto.Type.TYPE_MESSAGE)
                    .setTypeName(".example.Address"))
            // map<string, Address> homes; each value message is recursively validated.
            .addField(
                FieldDescriptorProto.newBuilder()
                    .setName("homes")
                    .setNumber(7)
                    .setLabel(FieldDescriptorProto.Label.LABEL_REPEATED)
                    .setType(FieldDescriptorProto.Type.TYPE_MESSAGE)
                    .setTypeName(".example.Person.HomesEntry"))
            // The synthetic map-entry type that a map<string, int32> field expands to.
            .addNestedType(
                DescriptorProto.newBuilder()
                    .setName("ScoresEntry")
                    .setOptions(MessageOptions.newBuilder().setMapEntry(true))
                    .addField(
                        FieldDescriptorProto.newBuilder()
                            .setName("key")
                            .setNumber(1)
                            .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL)
                            .setType(FieldDescriptorProto.Type.TYPE_STRING))
                    .addField(
                        FieldDescriptorProto.newBuilder()
                            .setName("value")
                            .setNumber(2)
                            .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL)
                            .setType(FieldDescriptorProto.Type.TYPE_INT32)))
            // The synthetic map-entry type for map<string, Address> homes.
            .addNestedType(
                DescriptorProto.newBuilder()
                    .setName("HomesEntry")
                    .setOptions(MessageOptions.newBuilder().setMapEntry(true))
                    .addField(
                        FieldDescriptorProto.newBuilder()
                            .setName("key")
                            .setNumber(1)
                            .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL)
                            .setType(FieldDescriptorProto.Type.TYPE_STRING))
                    .addField(
                        FieldDescriptorProto.newBuilder()
                            .setName("value")
                            .setNumber(2)
                            .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL)
                            .setType(FieldDescriptorProto.Type.TYPE_MESSAGE)
                            .setTypeName(".example.Address")))
            .build();

    FileDescriptorProto file =
        FileDescriptorProto.newBuilder()
            .setName("person.proto")
            .setSyntax("proto2")
            .setPackage("example")
            .addMessageType(address)
            .addMessageType(person)
            .build();

    Descriptors.FileDescriptor fileDescriptor =
        Descriptors.FileDescriptor.buildFrom(file, new Descriptors.FileDescriptor[] {});
    return fileDescriptor.findMessageTypeByName("Person");
  }

  /**
   * Builds an "Event" descriptor with a {@code google.protobuf.Timestamp created_at} field carrying
   * a {@code timestamp.lt} rule. Depends on the real {@code timestamp.proto} file descriptor.
   */
  private static Descriptors.Descriptor buildEventDescriptor() throws Exception {
    DescriptorProto event =
        DescriptorProto.newBuilder()
            .setName("Event")
            .addField(
                FieldDescriptorProto.newBuilder()
                    .setName("created_at")
                    .setNumber(1)
                    .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL)
                    .setType(FieldDescriptorProto.Type.TYPE_MESSAGE)
                    .setTypeName(".google.protobuf.Timestamp")
                    .setOptions(
                        fieldRule(
                            FieldRules.newBuilder()
                                .setTimestamp(
                                    TimestampRules.newBuilder()
                                        .setLt(Timestamp.newBuilder().setSeconds(1_000_000_000))))))
            .build();

    FileDescriptorProto file =
        FileDescriptorProto.newBuilder()
            .setName("event.proto")
            .setSyntax("proto2")
            .setPackage("example")
            .addDependency("google/protobuf/timestamp.proto")
            .addMessageType(event)
            .build();

    Descriptors.FileDescriptor fileDescriptor =
        Descriptors.FileDescriptor.buildFrom(
            file, new Descriptors.FileDescriptor[] {Timestamp.getDescriptor().getFile()});
    return fileDescriptor.findMessageTypeByName("Event");
  }

  /**
   * Builds a "Scalars" descriptor exercising bytes, enum, uint32, a {@code
   * google.protobuf.Int32Value} wrapper field, and a required oneof, each carrying a rule.
   */
  private static Descriptors.Descriptor buildScalarsDescriptor() throws Exception {
    com.google.protobuf.DescriptorProtos.EnumDescriptorProto color =
        com.google.protobuf.DescriptorProtos.EnumDescriptorProto.newBuilder()
            .setName("Color")
            .addValue(
                com.google.protobuf.DescriptorProtos.EnumValueDescriptorProto.newBuilder()
                    .setName("RED")
                    .setNumber(0))
            .addValue(
                com.google.protobuf.DescriptorProtos.EnumValueDescriptorProto.newBuilder()
                    .setName("GREEN")
                    .setNumber(1))
            .build();

    DescriptorProto scalars =
        DescriptorProto.newBuilder()
            .setName("Scalars")
            .addEnumType(color)
            // bytes data with bytes.min_len = 3.
            .addField(
                FieldDescriptorProto.newBuilder()
                    .setName("data")
                    .setNumber(1)
                    .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL)
                    .setType(FieldDescriptorProto.Type.TYPE_BYTES)
                    .setOptions(
                        fieldRule(
                            FieldRules.newBuilder()
                                .setBytes(
                                    build.buf.validate.BytesRules.newBuilder().setMinLen(3)))))
            // enum color with enum.const = 1 (GREEN).
            .addField(
                FieldDescriptorProto.newBuilder()
                    .setName("color")
                    .setNumber(2)
                    .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL)
                    .setType(FieldDescriptorProto.Type.TYPE_ENUM)
                    .setTypeName(".example.Scalars.Color")
                    .setOptions(
                        fieldRule(
                            FieldRules.newBuilder()
                                .setEnum(build.buf.validate.EnumRules.newBuilder().setConst(1)))))
            // uint32 count with uint32.gt = 10.
            .addField(
                FieldDescriptorProto.newBuilder()
                    .setName("count")
                    .setNumber(3)
                    .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL)
                    .setType(FieldDescriptorProto.Type.TYPE_UINT32)
                    .setOptions(
                        fieldRule(
                            FieldRules.newBuilder()
                                .setUint32(build.buf.validate.UInt32Rules.newBuilder().setGt(10)))))
            // google.protobuf.Int32Value wrapped with int32.gt = 0 on its inner value.
            .addField(
                FieldDescriptorProto.newBuilder()
                    .setName("wrapped")
                    .setNumber(4)
                    .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL)
                    .setType(FieldDescriptorProto.Type.TYPE_MESSAGE)
                    .setTypeName(".google.protobuf.Int32Value")
                    .setOptions(
                        fieldRule(
                            FieldRules.newBuilder().setInt32(Int32Rules.newBuilder().setGt(0)))))
            .build();

    FileDescriptorProto file =
        FileDescriptorProto.newBuilder()
            .setName("scalars.proto")
            .setSyntax("proto2")
            .setPackage("example")
            .addDependency("google/protobuf/wrappers.proto")
            .addMessageType(scalars)
            .build();

    Descriptors.FileDescriptor fileDescriptor =
        Descriptors.FileDescriptor.buildFrom(
            file,
            new Descriptors.FileDescriptor[] {
              com.google.protobuf.Int32Value.getDescriptor().getFile()
            });
    return fileDescriptor.findMessageTypeByName("Scalars");
  }

  /** Builds a "Choice" descriptor with a required {@code oneof choice { string a; string b; }}. */
  private static Descriptors.Descriptor buildOneofDescriptor() throws Exception {
    DescriptorProto choice =
        DescriptorProto.newBuilder()
            .setName("Choice")
            .addOneofDecl(
                com.google.protobuf.DescriptorProtos.OneofDescriptorProto.newBuilder()
                    .setName("choice")
                    .setOptions(
                        com.google.protobuf.DescriptorProtos.OneofOptions.newBuilder()
                            .setExtension(
                                ValidateProto.oneof,
                                build.buf.validate.OneofRules.newBuilder()
                                    .setRequired(true)
                                    .build())))
            .addField(
                FieldDescriptorProto.newBuilder()
                    .setName("a")
                    .setNumber(1)
                    .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL)
                    .setType(FieldDescriptorProto.Type.TYPE_STRING)
                    .setOneofIndex(0))
            .addField(
                FieldDescriptorProto.newBuilder()
                    .setName("b")
                    .setNumber(2)
                    .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL)
                    .setType(FieldDescriptorProto.Type.TYPE_STRING)
                    .setOneofIndex(0))
            .build();

    FileDescriptorProto file =
        FileDescriptorProto.newBuilder()
            .setName("choice.proto")
            .setSyntax("proto2")
            .setPackage("example")
            .addMessageType(choice)
            .build();

    Descriptors.FileDescriptor fileDescriptor =
        Descriptors.FileDescriptor.buildFrom(file, new Descriptors.FileDescriptor[] {});
    return fileDescriptor.findMessageTypeByName("Choice");
  }

  /** Builds a {@link MapValidateMessage} over {@code google.protobuf.Timestamp}. */
  private static MapValidateMessage timestampMessage(
      Descriptors.Descriptor eventDescriptor, long seconds) {
    Descriptors.Descriptor timestampDescriptor =
        eventDescriptor.findFieldByName("created_at").getMessageType();
    Map<Descriptors.FieldDescriptor, Object> fields = new HashMap<>();
    fields.put(timestampDescriptor.findFieldByName("seconds"), seconds);
    return new MapValidateMessage(timestampDescriptor, fields);
  }

  /** A {@code FieldOptions} carrying a {@code string.min_len} field rule. */
  private static FieldOptions stringMinLen(int minLen) {
    return fieldRule(FieldRules.newBuilder().setString(StringRules.newBuilder().setMinLen(minLen)));
  }

  /**
   * Wraps a {@link FieldRules} into {@code FieldOptions} via the {@code buf.validate.field} ext.
   */
  private static FieldOptions fieldRule(FieldRules.Builder rules) {
    return FieldOptions.newBuilder().setExtension(ValidateProto.field, rules.build()).build();
  }

  /** A {@link ValidateMessage} backed by an in-memory map of field values. */
  private static final class MapValidateMessage implements ValidateMessage {
    private final Descriptors.Descriptor descriptor;
    private final Map<Descriptors.FieldDescriptor, Object> fields;

    MapValidateMessage(
        Descriptors.Descriptor descriptor, Map<Descriptors.FieldDescriptor, Object> fields) {
      this.descriptor = descriptor;
      this.fields = fields;
    }

    @Override
    public Descriptors.Descriptor getDescriptorForType() {
      return descriptor;
    }

    @Override
    public boolean hasField(Descriptors.FieldDescriptor field) {
      return fields.containsKey(field);
    }

    @Override
    public Value getField(Descriptors.FieldDescriptor field) {
      Object value = fields.get(field);
      // A message-typed singular field is backed by another ValidateMessage; wrap it so the
      // embedded-message evaluator can recurse without a protobuf Message.
      if (value instanceof ValidateMessage) {
        return new ValidateMessageFieldValue(field, (ValidateMessage) value);
      }
      // Map fields need a runtime-independent Value: ObjectValue.mapValue() expects
      // protobuf map-entry messages, which an alternative runtime would not have.
      if (field.isMapField()) {
        Map<?, ?> map = value == null ? Collections.emptyMap() : (Map<?, ?>) value;
        return new MapFieldValue(field, map);
      }
      // Repeated fields: a custom Value so message elements recurse as ValidateMessages.
      if (field.isRepeated()) {
        List<?> list = value == null ? Collections.emptyList() : (List<?>) value;
        return new RepeatedFieldValue(field, list);
      }
      // A singular message field must be backed by a ValidateMessage (handled above). Reaching
      // here means a non-ValidateMessage value was stored for a message field, which would
      // otherwise fail later as a ClassCastException to com.google.protobuf.Message.
      if (isMessage(field)) {
        throw new IllegalArgumentException(
            "message field \""
                + field.getName()
                + "\" must be provided as a ValidateMessage, but got "
                + (value == null ? "null" : value.getClass().getName()));
      }
      if (value == null) {
        value = field.getDefaultValue();
      }
      return new ObjectValue(field, value);
    }

    @Override
    public Object celValue() {
      // Well-known types must surface their CEL-native form (e.g. Timestamp -> Instant)
      // so the standard WKT rules, which expect `this` to be that form, can evaluate.
      Object wellKnown = wellKnownCelValue();
      if (wellKnown != null) {
        return wellKnown;
      }
      // Otherwise: CEL's runtime dispatches field selection on the actual object, so a
      // plain Map keyed by field name works in place of a protobuf Message. Each field
      // delegates to its Value's own CEL conversion (nested messages, repeated, maps).
      Map<String, Object> out = new HashMap<>();
      for (Descriptors.FieldDescriptor field : fields.keySet()) {
        out.put(field.getName(), getField(field).celValue());
      }
      return out;
    }

    /**
     * Returns the CEL-native representation if this message wraps a well-known type, otherwise
     * {@code null}. Mirrors how {@code ProtoAdapter} converts these messages for the protobuf
     * runtime, but builds the value from the field map instead of a {@code Message}.
     */
    private @Nullable Object wellKnownCelValue() {
      switch (descriptor.getFullName()) {
        case "google.protobuf.Timestamp":
          return Instant.ofEpochSecond(longField("seconds"), longField("nanos"));
        case "google.protobuf.Duration":
          return Duration.ofSeconds(longField("seconds"), longField("nanos"));
        case "google.protobuf.DoubleValue":
        case "google.protobuf.FloatValue":
        case "google.protobuf.Int64Value":
        case "google.protobuf.UInt64Value":
        case "google.protobuf.Int32Value":
        case "google.protobuf.UInt32Value":
        case "google.protobuf.BoolValue":
        case "google.protobuf.StringValue":
        case "google.protobuf.BytesValue":
          Descriptors.FieldDescriptor valueField = descriptor.findFieldByName("value");
          Object raw = fields.get(valueField);
          return ProtoAdapter.scalarToCel(
              valueField.getType(), raw == null ? valueField.getDefaultValue() : raw);
        default:
          return null;
      }
    }

    /** Reads a numeric field as a long, defaulting to 0 when unset. */
    private long longField(String name) {
      Object value = fields.get(descriptor.findFieldByName(name));
      return value == null ? 0L : ((Number) value).longValue();
    }
  }

  /** A {@link Value} for a message-typed field, backed by a nested {@link ValidateMessage}. */
  private static final class ValidateMessageFieldValue implements Value {
    private final Descriptors.FieldDescriptor fieldDescriptor;
    private final ValidateMessage message;

    ValidateMessageFieldValue(
        Descriptors.FieldDescriptor fieldDescriptor, ValidateMessage message) {
      this.fieldDescriptor = fieldDescriptor;
      this.message = message;
    }

    @Override
    public Descriptors.FieldDescriptor fieldDescriptor() {
      return fieldDescriptor;
    }

    @Override
    public ValidateMessage messageValue() {
      return message;
    }

    @Override
    public Object rawValue() {
      return message;
    }

    @Override
    public Object celValue() {
      return message.celValue();
    }

    @Override
    public <T> T jvmValue(Class<T> clazz) {
      return clazz.cast(message.celValue());
    }

    @Override
    public List<Value> repeatedValue() {
      return Collections.emptyList();
    }

    @Override
    public Map<Value, Value> mapValue() {
      return Collections.emptyMap();
    }
  }

  /**
   * A {@link Value} for a map field, backed by a plain {@link Map} of raw key/value pairs. The
   * key/value field descriptors come from the synthetic map-entry type, so each pair is exposed as
   * an {@link ObjectValue} without needing protobuf map-entry messages.
   */
  private static final class MapFieldValue implements Value {
    private final Descriptors.FieldDescriptor fieldDescriptor;
    private final Descriptors.FieldDescriptor keyDescriptor;
    private final Descriptors.FieldDescriptor valueDescriptor;
    private final Map<?, ?> value;

    MapFieldValue(Descriptors.FieldDescriptor fieldDescriptor, Map<?, ?> value) {
      this.fieldDescriptor = fieldDescriptor;
      this.keyDescriptor = fieldDescriptor.getMessageType().findFieldByNumber(1);
      this.valueDescriptor = fieldDescriptor.getMessageType().findFieldByNumber(2);
      this.value = value;
    }

    @Override
    public Descriptors.FieldDescriptor fieldDescriptor() {
      return fieldDescriptor;
    }

    @Override
    public ValidateMessage messageValue() {
      return null;
    }

    @Override
    public Object rawValue() {
      return value;
    }

    @Override
    public Object celValue() {
      // Map keys are always scalar/enum; values may be messages (backed by ValidateMessages).
      Map<Object, Object> out = new HashMap<>(value.size());
      for (Map.Entry<?, ?> entry : value.entrySet()) {
        out.put(
            ProtoAdapter.scalarToCel(keyDescriptor.getType(), entry.getKey()),
            celValueOf(valueDescriptor, entry.getValue()));
      }
      return out;
    }

    @Override
    public <T> T jvmValue(Class<T> clazz) {
      return clazz.cast(value);
    }

    @Override
    public List<Value> repeatedValue() {
      return Collections.emptyList();
    }

    @Override
    public Map<Value, Value> mapValue() {
      Map<Value, Value> out = new HashMap<>(value.size());
      for (Map.Entry<?, ?> entry : value.entrySet()) {
        out.put(
            new ObjectValue(keyDescriptor, entry.getKey()),
            mapValueElement(valueDescriptor, entry.getValue()));
      }
      return out;
    }
  }

  /**
   * A {@link Value} for a repeated field, backed by a plain {@link List} of raw elements. Scalar
   * elements reuse {@link ListElementValue}; message elements are backed by ValidateMessages so
   * they recurse without a protobuf Message.
   */
  private static final class RepeatedFieldValue implements Value {
    private final Descriptors.FieldDescriptor fieldDescriptor;
    private final List<?> values;

    RepeatedFieldValue(Descriptors.FieldDescriptor fieldDescriptor, List<?> values) {
      this.fieldDescriptor = fieldDescriptor;
      this.values = values;
    }

    @Override
    public Descriptors.FieldDescriptor fieldDescriptor() {
      return fieldDescriptor;
    }

    @Override
    public ValidateMessage messageValue() {
      return null;
    }

    @Override
    public Object rawValue() {
      return values;
    }

    @Override
    public Object celValue() {
      List<Object> out = new ArrayList<>(values.size());
      for (Object element : values) {
        out.add(celValueOf(fieldDescriptor, element));
      }
      return out;
    }

    @Override
    public <T> T jvmValue(Class<T> clazz) {
      return clazz.cast(values);
    }

    @Override
    public List<Value> repeatedValue() {
      List<Value> out = new ArrayList<>(values.size());
      for (Object element : values) {
        out.add(repeatedElement(fieldDescriptor, element));
      }
      return out;
    }

    @Override
    public Map<Value, Value> mapValue() {
      return Collections.emptyMap();
    }
  }

  /** Whether a descriptor refers to a message (as opposed to a scalar or enum). */
  private static boolean isMessage(Descriptors.FieldDescriptor descriptor) {
    return descriptor.getJavaType() == Descriptors.FieldDescriptor.JavaType.MESSAGE;
  }

  /**
   * Wraps a repeated element as a {@link Value}: a {@link ValidateMessageFieldValue} for messages,
   * otherwise a {@link ListElementValue}, mirroring {@code ObjectValue.repeatedValue()}.
   */
  private static Value repeatedElement(Descriptors.FieldDescriptor descriptor, Object value) {
    if (isMessage(descriptor)) {
      return new ValidateMessageFieldValue(descriptor, (ValidateMessage) value);
    }
    return new ListElementValue(descriptor, value);
  }

  /**
   * Wraps a map value as a {@link Value}: a {@link ValidateMessageFieldValue} for messages,
   * otherwise an {@link ObjectValue}, mirroring {@code ObjectValue.mapValue()}.
   */
  private static Value mapValueElement(Descriptors.FieldDescriptor descriptor, Object value) {
    if (isMessage(descriptor)) {
      return new ValidateMessageFieldValue(descriptor, (ValidateMessage) value);
    }
    return new ObjectValue(descriptor, value);
  }

  /** Converts a single repeated element or map value to its CEL representation. */
  private static Object celValueOf(Descriptors.FieldDescriptor descriptor, Object value) {
    if (isMessage(descriptor)) {
      return ((ValidateMessage) value).celValue();
    }
    return ProtoAdapter.scalarToCel(descriptor.getType(), value);
  }
}
