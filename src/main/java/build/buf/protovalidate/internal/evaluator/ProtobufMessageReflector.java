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

import build.buf.protovalidate.MessageReflector;
import build.buf.protovalidate.Value;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;

class ProtobufMessageReflector implements MessageReflector {
  private final Message message;

  ProtobufMessageReflector(Message message) {
    this.message = message;
  }

  public Message getMessage() {
    return message;
  }

  @Override
  public boolean hasField(FieldDescriptor field) {
    return message.hasField(field);
  }

  @Override
  public Value getField(FieldDescriptor field) {
    return new FieldValue(field, message.getField(field));
  }
}
