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

import com.google.protobuf.Descriptors.FieldDescriptor;

/**
 * {@link MessageReflector} is a wrapper around a protobuf message that provides reflective access
 * to the underlying message.
 *
 * <p>{@link MessageReflector} is a runtime-independent interface. Any protobuf runtime that
 * implements this interface can wrap its messages and, along with their {@link
 * com.google.protobuf.Descriptors.Descriptor}s, protovalidate-java will be able to validate them.
 */
public interface MessageReflector {
  /**
   * Whether the wrapped message has the field described by the provided field descriptor.
   *
   * @param field The field descriptor to check for.
   * @return Whether the field is present.
   */
  boolean hasField(FieldDescriptor field);

  /**
   * Get the value described by the provided field descriptor.
   *
   * @param field The field descriptor for which to retrieve a value.
   * @return The value corresponding to the field descriptor.
   */
  Value getField(FieldDescriptor field);
}
