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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** Top-level {@link Value} backed by a user-supplied {@link ValidateMessage}. */
final class ValidateMessageValue implements Value {

  private final ValidateMessage message;

  ValidateMessageValue(ValidateMessage message) {
    this.message = message;
  }

  @Override
  public Descriptors.@Nullable FieldDescriptor fieldDescriptor() {
    return null;
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
    throw new UnsupportedOperationException(
        "jvmValue returns a raw scalar for native rule evaluation and is not supported on a message"
            + " value; a ValidateMessage.getField() implementation returned a message value for a"
            + " field evaluated as a scalar");
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
