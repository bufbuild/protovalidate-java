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

package build.buf.protovalidate.internal.evaluator;

import build.buf.protovalidate.Value;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.projectnessie.cel.common.ULong;

/** The {@link Value} type that contains a field descriptor and its value. */
public final class ObjectValue implements Value {

  /**
   * {@link com.google.protobuf.Descriptors.FieldDescriptor} is the field descriptor for the value.
   */
  private final Descriptors.FieldDescriptor fieldDescriptor;

  /** Object type since the object type is inferred from the field descriptor. */
  private final Object value;

  /**
   * Constructs a new {@link build.buf.protovalidate.internal.evaluator.ObjectValue}.
   *
   * @param fieldDescriptor The field descriptor for the value.
   * @param value The value associated with the field descriptor.
   */
  public ObjectValue(Descriptors.FieldDescriptor fieldDescriptor, Object value) {
    this.fieldDescriptor = fieldDescriptor;
    this.value = value;
  }

  @Nullable
  @Override
  public Message messageValue() {
    if (fieldDescriptor.getJavaType() == Descriptors.FieldDescriptor.JavaType.MESSAGE) {
      return (Message) value;
    }
    return null;
  }

  @Override
  public <T> T value(Class<T> clazz) {
    Descriptors.FieldDescriptor.Type type = fieldDescriptor.getType();
    if (!fieldDescriptor.isRepeated()
        && (type == Descriptors.FieldDescriptor.Type.UINT32
            || type == Descriptors.FieldDescriptor.Type.UINT64
            || type == Descriptors.FieldDescriptor.Type.FIXED32
            || type == Descriptors.FieldDescriptor.Type.FIXED64)) {
      /*
       * Java does not have native support for unsigned int/long or uint32/uint64 types.
       * To work with CEL's uint type in Java, special handling is required.
       *
       * When using uint32/uint64 in your protobuf objects or CEL expressions in Java,
       * wrap them with the org.projectnessie.cel.common.ULong type.
       */
      return clazz.cast(ULong.valueOf(((Number) value).longValue()));
    }
    return clazz.cast(value);
  }

  @Override
  public List<Value> repeatedValue() {
    List<Value> out = new ArrayList<>();
    if (fieldDescriptor.isRepeated()) {
      List<?> list = (List<?>) value;
      for (Object o : list) {
        out.add(new build.buf.protovalidate.internal.evaluator.ObjectValue(fieldDescriptor, o));
      }
    }
    return out;
  }

  @Override
  public Map<Value, Value> mapValue() {
    List<AbstractMessage> input =
        value instanceof List
            ? (List<AbstractMessage>) value
            : Collections.singletonList((AbstractMessage) value);

    Descriptors.FieldDescriptor keyDesc = fieldDescriptor.getMessageType().findFieldByNumber(1);
    Descriptors.FieldDescriptor valDesc = fieldDescriptor.getMessageType().findFieldByNumber(2);
    Map<Value, Value> out = new HashMap<>(input.size());
    for (AbstractMessage entry : input) {
      Object keyValue = entry.getField(keyDesc);
      Value keyJavaValue =
          new build.buf.protovalidate.internal.evaluator.ObjectValue(keyDesc, keyValue);

      Object valValue = entry.getField(valDesc);
      Value valJavaValue =
          new build.buf.protovalidate.internal.evaluator.ObjectValue(valDesc, valValue);

      out.put(keyJavaValue, valJavaValue);
    }

    return out;
  }
}
