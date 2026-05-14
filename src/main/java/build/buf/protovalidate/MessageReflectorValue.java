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

/** Top-level {@link Value} backed by a user-supplied {@link MessageReflector}. */
final class MessageReflectorValue implements Value {

  private final MessageReflector reflector;

  MessageReflectorValue(MessageReflector reflector) {
    this.reflector = reflector;
  }

  @Override
  public Descriptors.@Nullable FieldDescriptor fieldDescriptor() {
    return null;
  }

  @Override
  public MessageReflector messageValue() {
    return reflector;
  }

  @Override
  public Object celValue() {
    return reflector.celValue();
  }

  @Override
  public <T> T jvmValue(Class<T> clazz) {
    throw new UnsupportedOperationException();
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
