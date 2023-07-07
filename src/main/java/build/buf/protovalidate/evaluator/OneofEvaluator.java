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
import build.buf.validate.Violation;
import com.google.protobuf.Descriptors.OneofDescriptor;
import com.google.protobuf.Message;

public class OneofEvaluator implements MessageEvaluator {

    // Descriptor is the OneofDescriptor targeted by this evaluator
    private final OneofDescriptor descriptor;
    // Required indicates that a member of the oneof must be set
    private final boolean required;

    public OneofEvaluator(OneofDescriptor descriptor, boolean required) {
        this.descriptor = descriptor;
        this.required = required;
    }

    @Override
    public boolean tautology() {
        return !required;
    }

    @Override
    public ValidationResult evaluate(Value val, boolean failFast) throws ExecutionException {
        return evaluateMessage(val.messageValue(), failFast);
    }

    @Override
    public void append(Evaluator eval) {
        throw new UnsupportedOperationException("append not supported for Oneof");
    }

    @Override
    public ValidationResult evaluateMessage(Message message, boolean failFast) throws ExecutionException {
        if (required && (message.getOneofFieldDescriptor(descriptor) == null)) {
            ValidationResult evalResult = new ValidationResult();
            Violation violation = Violation.newBuilder()
                    .setFieldPath(descriptor.getName())
                    .setConstraintId("required")
                    .setMessage("exactly one field is required in oneof")
                    .build();
            evalResult.addViolation(violation);
            return evalResult;
        }
        return new ValidationResult();
    }
}
