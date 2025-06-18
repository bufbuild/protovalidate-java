// Copyright 2023-2025 Buf Technologies, Inc.
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

import build.buf.validate.FieldRules;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.OneofDescriptor;
import dev.cel.common.types.CelType;
import dev.cel.common.types.CelTypes;
import dev.cel.common.types.ListType;
import dev.cel.common.types.MapType;
import dev.cel.common.types.SimpleType;
import dev.cel.common.types.StructTypeReference;
import dev.cel.common.types.UnspecifiedType;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * DescriptorMappings provides mappings between protocol buffer descriptors and CEL declarations.
 */
final class DescriptorMappings {
  /** Provides a {@link Descriptor} for {@link FieldRules}. */
  static final Descriptor FIELD_RULES_DESC = FieldRules.getDescriptor();

  /** Provides the {@link OneofDescriptor} for the type union in {@link FieldRules}. */
  static final OneofDescriptor FIELD_RULES_ONEOF_DESC = FIELD_RULES_DESC.getOneofs().get(0);

  /** Provides the {@link FieldDescriptor} for the map standard rules. */
  static final FieldDescriptor MAP_FIELD_RULES_DESC = FIELD_RULES_DESC.findFieldByName("map");

  /** Provides the {@link FieldDescriptor} for the repeated standard rules. */
  static final FieldDescriptor REPEATED_FIELD_RULES_DESC =
      FIELD_RULES_DESC.findFieldByName("repeated");

  /** Maps protocol buffer field kinds to their expected field rules. */
  static final Map<FieldDescriptor.Type, FieldDescriptor> EXPECTED_STANDARD_RULES = new HashMap<>();

  /**
   * Returns the {@link build.buf.validate.FieldRules} field that is expected for the given wrapper
   * well-known type's full name. If ok is false, no standard rules exist for that type.
   */
  static final Map<String, FieldDescriptor> EXPECTED_WKT_RULES = new HashMap<>();

  static {
    EXPECTED_STANDARD_RULES.put(
        FieldDescriptor.Type.FLOAT, FIELD_RULES_DESC.findFieldByName("float"));
    EXPECTED_STANDARD_RULES.put(
        FieldDescriptor.Type.DOUBLE, FIELD_RULES_DESC.findFieldByName("double"));
    EXPECTED_STANDARD_RULES.put(
        FieldDescriptor.Type.INT32, FIELD_RULES_DESC.findFieldByName("int32"));
    EXPECTED_STANDARD_RULES.put(
        FieldDescriptor.Type.INT64, FIELD_RULES_DESC.findFieldByName("int64"));
    EXPECTED_STANDARD_RULES.put(
        FieldDescriptor.Type.UINT32, FIELD_RULES_DESC.findFieldByName("uint32"));
    EXPECTED_STANDARD_RULES.put(
        FieldDescriptor.Type.UINT64, FIELD_RULES_DESC.findFieldByName("uint64"));
    EXPECTED_STANDARD_RULES.put(
        FieldDescriptor.Type.SINT32, FIELD_RULES_DESC.findFieldByName("sint32"));
    EXPECTED_STANDARD_RULES.put(
        FieldDescriptor.Type.SINT64, FIELD_RULES_DESC.findFieldByName("sint64"));
    EXPECTED_STANDARD_RULES.put(
        FieldDescriptor.Type.FIXED32, FIELD_RULES_DESC.findFieldByName("fixed32"));
    EXPECTED_STANDARD_RULES.put(
        FieldDescriptor.Type.FIXED64, FIELD_RULES_DESC.findFieldByName("fixed64"));
    EXPECTED_STANDARD_RULES.put(
        FieldDescriptor.Type.SFIXED32, FIELD_RULES_DESC.findFieldByName("sfixed32"));
    EXPECTED_STANDARD_RULES.put(
        FieldDescriptor.Type.SFIXED64, FIELD_RULES_DESC.findFieldByName("sfixed64"));
    EXPECTED_STANDARD_RULES.put(
        FieldDescriptor.Type.BOOL, FIELD_RULES_DESC.findFieldByName("bool"));
    EXPECTED_STANDARD_RULES.put(
        FieldDescriptor.Type.STRING, FIELD_RULES_DESC.findFieldByName("string"));
    EXPECTED_STANDARD_RULES.put(
        FieldDescriptor.Type.BYTES, FIELD_RULES_DESC.findFieldByName("bytes"));
    EXPECTED_STANDARD_RULES.put(
        FieldDescriptor.Type.ENUM, FIELD_RULES_DESC.findFieldByName("enum"));

    EXPECTED_WKT_RULES.put("google.protobuf.Any", FIELD_RULES_DESC.findFieldByName("any"));
    EXPECTED_WKT_RULES.put(
        "google.protobuf.Duration", FIELD_RULES_DESC.findFieldByName("duration"));
    EXPECTED_WKT_RULES.put(
        "google.protobuf.Timestamp", FIELD_RULES_DESC.findFieldByName("timestamp"));
  }

  private DescriptorMappings() {}

  /**
   * Returns the {@link FieldRules} field that is expected for the given protocol buffer field kind.
   *
   * @param fqn Fully qualified name of protobuf value wrapper.
   * @return The rules field descriptor for the specified wrapper fully qualified name.
   */
  @Nullable
  static FieldDescriptor expectedWrapperRules(String fqn) {
    switch (fqn) {
      case "google.protobuf.BoolValue":
        return EXPECTED_STANDARD_RULES.get(FieldDescriptor.Type.BOOL);
      case "google.protobuf.BytesValue":
        return EXPECTED_STANDARD_RULES.get(FieldDescriptor.Type.BYTES);
      case "google.protobuf.DoubleValue":
        return EXPECTED_STANDARD_RULES.get(FieldDescriptor.Type.DOUBLE);
      case "google.protobuf.FloatValue":
        return EXPECTED_STANDARD_RULES.get(FieldDescriptor.Type.FLOAT);
      case "google.protobuf.Int32Value":
        return EXPECTED_STANDARD_RULES.get(FieldDescriptor.Type.INT32);
      case "google.protobuf.Int64Value":
        return EXPECTED_STANDARD_RULES.get(FieldDescriptor.Type.INT64);
      case "google.protobuf.StringValue":
        return EXPECTED_STANDARD_RULES.get(FieldDescriptor.Type.STRING);
      case "google.protobuf.UInt32Value":
        return EXPECTED_STANDARD_RULES.get(FieldDescriptor.Type.UINT32);
      case "google.protobuf.UInt64Value":
        return EXPECTED_STANDARD_RULES.get(FieldDescriptor.Type.UINT64);
      default:
        return null;
    }
  }

  /**
   * Maps a {@link FieldDescriptor.Type} to a compatible {@link com.google.api.expr.v1alpha1.Type}.
   *
   * @param kind The protobuf field type.
   * @return The corresponding CEL type for the protobuf field.
   */
  static CelType protoKindToCELType(FieldDescriptor.Type kind) {
    switch (kind) {
      case FLOAT:
      case DOUBLE:
        return SimpleType.DOUBLE;
      case INT32:
      case INT64:
      case SINT32:
      case SINT64:
      case SFIXED32:
      case SFIXED64:
      case ENUM:
        return SimpleType.INT;
      case UINT32:
      case UINT64:
      case FIXED32:
      case FIXED64:
        return SimpleType.UINT;
      case BOOL:
        return SimpleType.BOOL;
      case STRING:
        return SimpleType.STRING;
      case BYTES:
        return SimpleType.BYTES;
      case MESSAGE:
      case GROUP:
        return StructTypeReference.create(kind.getJavaType().name());
      default:
        return UnspecifiedType.create();
    }
  }

  /**
   * Produces the field descriptor from the {@link FieldRules} 'type' oneof that matches the
   * provided target field descriptor. If the returned value is null, the field does not expect any
   * standard rules.
   */
  @Nullable
  static FieldDescriptor getExpectedRuleDescriptor(
      FieldDescriptor fieldDescriptor, boolean forItems) {
    if (fieldDescriptor.isMapField()) {
      return DescriptorMappings.MAP_FIELD_RULES_DESC;
    } else if (fieldDescriptor.isRepeated() && !forItems) {
      return DescriptorMappings.REPEATED_FIELD_RULES_DESC;
    } else if (fieldDescriptor.getJavaType() == FieldDescriptor.JavaType.MESSAGE) {
      return DescriptorMappings.EXPECTED_WKT_RULES.get(
          fieldDescriptor.getMessageType().getFullName());
    } else {
      return DescriptorMappings.EXPECTED_STANDARD_RULES.get(fieldDescriptor.getType());
    }
  }

  /**
   * Resolves the CEL value type for the provided {@link FieldDescriptor}. If forItems is true, the
   * type for the repeated list items is returned instead of the list type itself.
   */
  static CelType getCELType(FieldDescriptor fieldDescriptor, boolean forItems) {
    if (!forItems) {
      if (fieldDescriptor.isMapField()) {
        return MapType.create(
            getCELType(fieldDescriptor.getMessageType().findFieldByNumber(1), true),
            getCELType(fieldDescriptor.getMessageType().findFieldByNumber(2), true));
      } else if (fieldDescriptor.isRepeated()) {
        return ListType.create(getCELType(fieldDescriptor, true));
      }
    }

    if (fieldDescriptor.getJavaType() == FieldDescriptor.JavaType.MESSAGE) {
      String fqn = fieldDescriptor.getMessageType().getFullName();
      return CelTypes.getWellKnownCelType(fqn).orElse(StructTypeReference.create(fqn));
    }
    return DescriptorMappings.protoKindToCELType(fieldDescriptor.getType());
  }
}
