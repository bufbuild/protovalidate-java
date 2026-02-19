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

import com.google.protobuf.Descriptors;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * {@link Value} is a wrapper around a protobuf value that provides helper methods for accessing the
 * value.
 */
public interface Value {
  /**
   * Get the field descriptor that corresponds to the underlying Value, if it is a message field.
   *
   * @return The underlying {@link Descriptors.FieldDescriptor}. null if the underlying value is not
   *     a message field.
   */
  Descriptors.@Nullable FieldDescriptor fieldDescriptor();

  /**
   * Get the underlying value as a {@link MessageReflector} type.
   *
   * @return The underlying {@link MessageReflector} value. null if the underlying value is not a {@link
   *     MessageReflector} type.
   */
  @Nullable MessageReflector messageValue();

  /**
   * Get the underlying value as a list.
   *
   * @return The underlying value as a list. Empty list is returned if the underlying type is not a
   *     list.
   */
  List<Value> repeatedValue();

  /**
   * Get the underlying value as a map.
   *
   * @return The underlying value as a map. Empty map is returned if the underlying type is not a
   *     list.
   */
  Map<Value, Value> mapValue();

  /**
   * Get the underlying value as it should be provided to CEL.
   *
   * @return The underlying value as a CEL-compatible type.
   */
  Object celValue();

  /**
   * Get the underlying value and cast it to the class type, which will be a type checkable
   * internally by protovalidate-java.
   *
   * @param clazz The inferred class.
   * @return The value cast to the inferred class type.
   * @param <T> The class type.
   */
  <T> T jvmValue(Class<T> clazz);
}
