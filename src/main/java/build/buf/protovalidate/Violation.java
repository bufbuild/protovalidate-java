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
import org.jspecify.annotations.Nullable;

/** {@link Violation} provides all the collected information about an individual rule violation. */
public interface Violation {
  /** {@link FieldValue} represents a Protobuf field value inside a Protobuf message. */
  interface FieldValue {
    /**
     * Gets the value of the field, which may be null, a primitive, a Map or a List.
     *
     * @return The value of the protobuf field.
     */
    @Nullable Object getValue();

    /**
     * Gets the field descriptor of the field this value is from.
     *
     * @return A FieldDescriptor pertaining to this field.
     */
    Descriptors.FieldDescriptor getDescriptor();
  }

  /**
   * Gets the protobuf form of this violation.
   *
   * @return The protobuf form of this violation.
   */
  build.buf.validate.Violation toProto();

  /**
   * Gets the value of the field this violation pertains to, or null if there is none.
   *
   * @return Value of the field associated with the violation, or null if there is none.
   */
  @Nullable FieldValue getFieldValue();

  /**
   * Gets the value of the rule this violation pertains to, or null if there is none.
   *
   * @return Value of the rule associated with the violation, or null if there is none.
   */
  @Nullable FieldValue getRuleValue();
}
