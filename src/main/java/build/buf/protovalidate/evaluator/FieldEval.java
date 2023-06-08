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

import build.buf.protovalidate.errors.ValidationError;
import build.buf.validate.Violation;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;
import lombok.Data;

@Data
public class FieldEval implements MessageEvaluator {
    private Value value;
    private FieldDescriptor descriptor;
    private boolean required;
    private boolean optional;

    public FieldEval(FieldDescriptor descriptor, boolean required, boolean optional) {
        this.value = new Value();
        this.descriptor = descriptor;
        this.required = required;
        this.optional = optional;
    }

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
    public void evaluate(DynamicMessage val, boolean failFast) throws ValidationError {
        evaluateMessage(val, failFast);
    }

    @Override
    public void evaluateMessage(Message message, boolean failFast) throws ValidationError {
        if (required && !message.hasField(descriptor)) {
            ValidationError err = new ValidationError();
            err.addViolation(Violation.newBuilder()
                    .setFieldPath(descriptor.getName())
                    .setConstraintId("required")
                    .setMessage("value is required")
                    .build());
            throw err;
        }

        if ((optional || value.isIgnoreEmpty()) && !message.hasField(descriptor)) {
            return;
        }

        Object fieldValue = message.getField(descriptor);
        value.evaluate((DynamicMessage) fieldValue, failFast);
    }

    @Override
    public void append(MessageEvaluator eval) {

    }
}
