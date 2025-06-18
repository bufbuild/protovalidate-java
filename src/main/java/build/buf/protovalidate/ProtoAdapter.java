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

import com.google.common.primitives.UnsignedLong;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.Descriptors;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CEL supports protobuf natively but when we pass it field values (like scalars, repeated, and
 * maps) it has no way to treat them like a proto message field. This class has methods to convert
 * to a cel values.
 */
final class ProtoAdapter {
  /** Converts a protobuf field value to CEL compatible value. */
  static Object toCel(Descriptors.FieldDescriptor fieldDescriptor, Object value) {
    Descriptors.FieldDescriptor.Type type = fieldDescriptor.getType();
    if (fieldDescriptor.isMapField()) {
      List<AbstractMessage> input =
          value instanceof List
              ? (List<AbstractMessage>) value
              : Collections.singletonList((AbstractMessage) value);
      Descriptors.FieldDescriptor keyDesc = fieldDescriptor.getMessageType().findFieldByNumber(1);
      Descriptors.FieldDescriptor valDesc = fieldDescriptor.getMessageType().findFieldByNumber(2);
      Map<Object, Object> out = new HashMap<>(input.size());

      for (AbstractMessage entry : input) {
        Object keyValue = entry.getField(keyDesc);
        Object valValue = entry.getField(valDesc);
        out.put(toCel(keyDesc, keyValue), toCel(valDesc, valValue));
      }
      return out;
    }
    // Cel understands protobuf message so we return as is (even if it is repeated).
    if (type == Descriptors.FieldDescriptor.Type.MESSAGE) {
      return value;
    }
    if (fieldDescriptor.isRepeated()) {
      List<Object> out = new ArrayList<>();
      List<?> list = (List<?>) value;
      for (Object element : list) {
        out.add(scalarToCel(type, element));
      }
      return out;
    }
    return scalarToCel(type, value);
  }

  /** Converts a scalar type to cel value. */
  static Object scalarToCel(Descriptors.FieldDescriptor.Type type, Object value) {
    switch (type) {
      case ENUM:
        if (value instanceof Descriptors.EnumValueDescriptor) {
          return (long) ((Descriptors.EnumValueDescriptor) value).getNumber();
        }
        return value;
      case FLOAT:
        return Double.valueOf((Float) value);
      case INT32:
      case SINT32:
      case SFIXED32:
        return Long.valueOf((Integer) value);
      case FIXED32:
      case UINT32:
        return UnsignedLong.fromLongBits(Long.valueOf((Integer) value));
      case UINT64:
      case FIXED64:
        return UnsignedLong.fromLongBits((Long) value);
      default:
        return value;
    }
  }
}
