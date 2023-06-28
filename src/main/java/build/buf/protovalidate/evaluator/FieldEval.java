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

import build.buf.protovalidate.ValidationResult;
import build.buf.protovalidate.errors.ValidationError;
import build.buf.validate.Violation;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.MapEntry;
import com.google.protobuf.Message;

import java.util.ArrayList;
import java.util.List;

public class FieldEval implements MessageEvaluator {
    public final Value value;
    private final FieldDescriptor descriptor;
    private final boolean required;
    private final boolean optional;

    public FieldEval(Value value, FieldDescriptor descriptor, boolean required, boolean optional) {
        this.value = value;
        this.descriptor = descriptor;
        this.required = required;
        this.optional = optional;
    }

    public boolean tautology() {
        return !required && value.tautology();
    }

    @Override
    public ValidationResult evaluate(JavaValue val, boolean failFast) {
        return evaluateMessage(val.value(), failFast);
    }

    @Override
    public ValidationResult evaluateMessage(Message message, boolean failFast) throws ValidationError {
        boolean hasField;
        // TODO: how does this behave in other descriptor value types like map?
        if (descriptor.isRepeated()) {
            hasField = message.getRepeatedFieldCount(descriptor) != 0;
        } else {
            hasField = message.hasField(descriptor);
        }
        if (required && !hasField) {
            ValidationError err = new ValidationError();
            err.addViolation(Violation.newBuilder()
                    .setFieldPath(descriptor.getName())
                    .setConstraintId("required")
                    .setMessage("value is required")
                    .build());
            return new ValidationResult(err);
        }

        if ((optional || value.ignoreEmpty) && !hasField) {
            return ValidationResult.success();
        }
        Object fieldValue = message.getField(descriptor);
        ValidationResult evaluate = value.evaluate(new JavaValue(descriptor, fieldValue), failFast);
        if (evaluate.isFailure()) {
            evaluate.prefixErrorPaths(descriptor.getName());
        }
        return evaluate;
    }

    @Override
    public void append(MessageEvaluator eval) {
        throw new UnsupportedOperationException("append not supported for FieldEval");
    }

    @Override
    public void append(Evaluator eval) {
        throw new UnsupportedOperationException("append not supported for FieldEval");
    }

    public Value getValue() {
        return value;
    }
}
