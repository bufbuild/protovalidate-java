// Copyright 2023 Buf Technologies, Inc.
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

package build.buf.protovalidate.evaluator;

import build.buf.protovalidate.results.ExecutionException;
import build.buf.protovalidate.results.ValidationResult;
import build.buf.gen.buf.validate.Violation;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;

class FieldEvaluator implements MessageEvaluator {
    public final ValueEvaluator valueEvaluator;
    private final FieldDescriptor descriptor;
    private final boolean required;
    private final boolean optional;

    FieldEvaluator(ValueEvaluator valueEvaluator, FieldDescriptor descriptor, boolean required, boolean optional) {
        this.valueEvaluator = valueEvaluator;
        this.descriptor = descriptor;
        this.required = required;
        this.optional = optional;
    }

    @Override
    public boolean tautology() {
        return !required && valueEvaluator.tautology();
    }

    @Override
    public ValidationResult evaluate(Value val, boolean failFast) throws ExecutionException {
        return evaluateMessage(val.messageValue(), failFast);
    }

    @Override
    public ValidationResult evaluateMessage(Message message, boolean failFast) throws ExecutionException {
        boolean hasField;
        // TODO: how does this behave in other descriptor value types like map?
        if (descriptor.isRepeated()) {
            hasField = message.getRepeatedFieldCount(descriptor) != 0;
        } else {
            hasField = message.hasField(descriptor);
        }
        if (required && !hasField) {
            ValidationResult evalResult = new ValidationResult();
            evalResult.addViolation(Violation.newBuilder()
                    .setFieldPath(descriptor.getName())
                    .setConstraintId("required")
                    .setMessage("value is required")
                    .build());
            return evalResult;
        }

        if ((optional || valueEvaluator.ignoreEmpty) && !hasField) {
            return new ValidationResult();
        }
        Object fieldValue = message.getField(descriptor);
        ValidationResult evalResult = valueEvaluator.evaluate(new Value(descriptor, fieldValue), failFast);
        evalResult.prefixErrorPaths("%s", descriptor.getName());
        return evalResult;
    }

    @Override
    public void append(Evaluator eval) {
        throw new UnsupportedOperationException("append not supported for FieldEval");
    }
}
