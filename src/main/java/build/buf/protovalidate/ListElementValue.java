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

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * The {@link Value} type that contains a field descriptor for repeated field and the value of an
 * element.
 */
final class ListElementValue implements Value {
  /** Object type since the object type is inferred from the field descriptor. */
  private final Object value;

  /**
   * {@link com.google.protobuf.Descriptors.FieldDescriptor} is the field descriptor for the value.
   */
  private final Descriptors.FieldDescriptor fieldDescriptor;

  ListElementValue(Descriptors.FieldDescriptor fieldDescriptor, Object value) {
    this.value = value;
    this.fieldDescriptor = fieldDescriptor;
  }

  @Override
  public Descriptors.@Nullable FieldDescriptor fieldDescriptor() {
    return fieldDescriptor;
  }

  @Override
  public @Nullable Message messageValue() {
    if (fieldDescriptor.getJavaType() == Descriptors.FieldDescriptor.JavaType.MESSAGE) {
      return (Message) value;
    }
    return null;
  }

  @Override
  public <T> T value(Class<T> clazz) {
    Descriptors.FieldDescriptor.Type type = fieldDescriptor.getType();
    if (type == Descriptors.FieldDescriptor.Type.MESSAGE) {
      return clazz.cast(value);
    }
    return clazz.cast(ProtoAdapter.scalarToCel(type, value));
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
