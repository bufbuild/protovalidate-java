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

package build.buf.protovalidate.rules;

import build.buf.protovalidate.Evaluator;
import build.buf.protovalidate.ObjectValue;
import build.buf.protovalidate.RuleViolation;
import build.buf.protovalidate.Value;
import build.buf.protovalidate.exceptions.ExecutionException;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import java.util.List;

/**
 * Adapter that lets a scalar-rule evaluator run against a {@code google.protobuf.*Value} wrapper
 * field. At evaluation time it pulls the inner {@code value} field off the wrapper {@link Message}
 * and delegates to the wrapped scalar evaluator.
 *
 * <p>CEL's runtime auto-unwraps wrappers to their inner primitive type, so the CEL path doesn't
 * need this adapter. Native evaluators expect the underlying scalar (e.g. {@code Long} for {@code
 * Int64Value.value}); without unwrapping they'd see the wrapper {@link Message} and misbehave.
 * Mirrors {@code wrappedValueEval} in protovalidate-go's {@code builder.go}.
 *
 * <p>The wrapped evaluator's {@link RuleBase} is constructed against the OUTER wrapper field's
 * {@code ValueEvaluator}, so violation field paths point at the user's wrapper-typed field rather
 * than the synthetic inner {@code value}.
 */
final class WrappedValueEvaluator implements Evaluator {
  private final FieldDescriptor innerField;
  private final Evaluator inner;

  WrappedValueEvaluator(FieldDescriptor innerField, Evaluator inner) {
    this.innerField = innerField;
    this.inner = inner;
  }

  @Override
  public boolean tautology() {
    return inner.tautology();
  }

  @Override
  public List<RuleViolation.Builder> evaluate(Value val, boolean failFast)
      throws ExecutionException {
    Message message = val.messageValue();
    if (message == null) {
      // proto3 message-typed field absent — no value to validate.
      return RuleViolation.NO_VIOLATIONS;
    }
    Object innerValue = message.getField(innerField);
    return inner.evaluate(new ObjectValue(innerField, innerValue), failFast);
  }
}
