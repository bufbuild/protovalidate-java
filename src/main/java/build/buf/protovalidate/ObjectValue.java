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

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** The {@link Value} type that contains a field descriptor and its value. */
final class ObjectValue implements Value {

  /**
   * {@link com.google.protobuf.Descriptors.FieldDescriptor} is the field descriptor for the value.
   */
  private final Descriptors.FieldDescriptor fieldDescriptor;

  /** Object type since the object type is inferred from the field descriptor. */
  private final Object value;

  /**
   * Constructs a new {@link ObjectValue}.
   *
   * @param fieldDescriptor The field descriptor for the value.
   * @param value The value associated with the field descriptor.
   */
  ObjectValue(Descriptors.FieldDescriptor fieldDescriptor, Object value) {
    this.fieldDescriptor = fieldDescriptor;
    this.value = value;
  }

  @Override
  public Descriptors.FieldDescriptor fieldDescriptor() {
    return fieldDescriptor;
  }

  @Nullable
  @Override
  public MessageReflector messageValue() {
    if (fieldDescriptor.getJavaType() == Descriptors.FieldDescriptor.JavaType.MESSAGE) {
      return new ProtobufMessageReflector((Message) value);
    }
    return null;
  }

  @Override
  public Object celValue() {
      return ProtoAdapter.toCel(fieldDescriptor, value);
  }

  @Override
  public <T> T jvmValue(Class<T> clazz) {
    if (value instanceof Descriptors.EnumValueDescriptor) {
      return clazz.cast(((Descriptors.EnumValueDescriptor) value).getNumber());
    }
    return clazz.cast(ProtoAdapter.toCel(fieldDescriptor, value));
  }

  @Override
  public List<Value> repeatedValue() {
    List<Value> out = new ArrayList<>();
    if (fieldDescriptor.isRepeated()) {
      List<?> list = (List<?>) value;
      for (Object o : list) {
        out.add(new ListElementValue(fieldDescriptor, o));
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
      Value keyJavaValue = new ObjectValue(keyDesc, keyValue);

      Object valValue = entry.getField(valDesc);
      Value valJavaValue = new ObjectValue(valDesc, valValue);

      out.put(keyJavaValue, valJavaValue);
    }

    return out;
  }
}
