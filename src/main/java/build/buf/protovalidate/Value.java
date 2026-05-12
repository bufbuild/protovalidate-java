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

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * {@link Value} is a wrapper around a protobuf value that provides helper methods for accessing the
 * value.
 */
interface Value {
  /**
   * Get the field descriptor that corresponds to the underlying Value, if it is a message field.
   *
   * @return The underlying {@link Descriptors.FieldDescriptor}. null if the underlying value is not
   *     a message field.
   */
  Descriptors.@Nullable FieldDescriptor fieldDescriptor();

  /**
   * Get the underlying value as a {@link Message} type.
   *
   * @return The underlying {@link Message} value. null if the underlying value is not a {@link
   *     Message} type.
   */
  @Nullable Message messageValue();

  /**
   * Get the underlying value and cast it to the class type.
   *
   * @param clazz The inferred class.
   * @return The value casted to the inferred class type.
   * @param <T> The class type.
   */
  <T> T value(Class<T> clazz);

  /**
   * Returns the underlying protobuf Java value without any CEL-specific adaptation.
   *
   * <p>{@link #value(Class)} routes scalars through {@code ProtoAdapter.toCel}, which converts
   * {@code int32→Long}, {@code uint32→UnsignedLong}, {@code float→Double}, {@code bytes→
   * CelByteString}, etc. — appropriate for the CEL evaluation path but lossy for native rule
   * evaluators that compare against raw protobuf field values. Native evaluators in {@code
   * build.buf.protovalidate.rules} use this method to obtain values they can compare directly with
   * the values they read off the typed rule message.
   *
   * @return The underlying value as protobuf-java provides it. Non-null for all values produced by
   *     the evaluator pipeline (field reads, list elements, message wrappers — all guarantee a
   *     value).
   */
  Object rawValue();

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
}
