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
import com.google.protobuf.Message;
import dev.cel.common.values.CelValue;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * A {@link Value} backed by a {@link CelValue} for CEL evaluation and a protobuf {@link Message}
 * for structural validation (field presence, required checks, etc.).
 *
 * <p>When CEL evaluates expressions against this value, it receives the CelValue (typically a
 * StructValue) which can navigate fields lazily. For non-CEL evaluators that need protobuf Message
 * access (e.g., FieldEvaluator for presence checks), the underlying Message is provided.
 */
final class CelBackedMessageValue implements Value {
  private final CelValue celValue;
  private final Message message;

  CelBackedMessageValue(CelValue celValue, Message message) {
    this.celValue = celValue;
    this.message = message;
  }

  @Override
  public Descriptors.@Nullable FieldDescriptor fieldDescriptor() {
    return null;
  }

  @Override
  public Message messageValue() {
    return message;
  }

  @Override
  public <T> T value(Class<T> clazz) {
    // CEL receives the CelValue; it knows how to navigate StructValues via selectField
    return clazz.cast(celValue);
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
