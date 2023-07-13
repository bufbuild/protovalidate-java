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

import build.buf.gen.buf.validate.Violation;
import build.buf.protovalidate.results.ExecutionException;
import build.buf.protovalidate.results.ValidationResult;
import com.google.protobuf.Descriptors.OneofDescriptor;
import com.google.protobuf.Message;

/**
 * {@link OneofEvaluator} performs validation on a oneof union.
 */
public class OneofEvaluator implements Evaluator {
    /**
     * The {@link OneofDescriptor} targeted by this evaluator.
     */
    private final OneofDescriptor descriptor;
    /**
     * Indicates that a member of the oneof must be set.
     */
    private final boolean required;

    /**
     * Constructs a {@link OneofEvaluator}.
     */
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
        Message message = val.messageValue();
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

    @Override
    public void append(Evaluator eval) {
        throw new UnsupportedOperationException("append not supported for Oneof");
    }

}