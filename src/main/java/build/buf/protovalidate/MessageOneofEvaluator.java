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

import build.buf.protovalidate.exceptions.ExecutionException;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A specialized {@link Evaluator} for applying {@code buf.validate.MessageOneofRule} to a {@link
 * com.google.protobuf.Message}.
 */
final class MessageOneofEvaluator implements Evaluator {
  /** List of fields that are part of the oneof */
  final List<FieldDescriptor> fields;

  /** If at least one must be set. */
  final boolean required;

  MessageOneofEvaluator(List<FieldDescriptor> fields, boolean required) {
    this.fields = fields;
    this.required = required;
  }

  @Override
  public boolean tautology() {
    return false;
  }

  @Override
  public List<RuleViolation.Builder> evaluate(Value val, boolean failFast)
      throws ExecutionException {
    Message msg = val.messageValue();
    if (msg == null) {
      return RuleViolation.NO_VIOLATIONS;
    }
    int hasCount = 0;
    for (FieldDescriptor field : fields) {
      if (msg.hasField(field)) {
        hasCount++;
      }
    }
    if (hasCount > 1) {
      return Collections.singletonList(
          RuleViolation.newBuilder()
              .setRuleId("message.oneof")
              .setMessage(String.format("only one of %s can be set", fieldNames())));
    }
    if (this.required && hasCount == 0) {
      return Collections.singletonList(
          RuleViolation.newBuilder()
              .setRuleId("message.oneof")
              .setMessage(String.format("one of %s must be set", fieldNames())));
    }
    return Collections.emptyList();
  }

  String fieldNames() {
    return fields.stream().map(FieldDescriptor::getName).collect(Collectors.joining(", "));
  }
}
