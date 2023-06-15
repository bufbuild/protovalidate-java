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
import com.google.protobuf.Message;

import java.util.ArrayList;
import java.util.List;

public class MessageEvaluatorImpl implements MessageEvaluator {

    // Err stores if there was a compilation error constructing this evaluator.
    // It is cached here so that it can be stored in the registry's lookup table.
    private Exception err;

    // evaluators are the individual evaluators that are applied to a message.
    private List<MessageEvaluator> evaluators = new ArrayList<>();

    public MessageEvaluatorImpl() {
    }

    @Override
    public boolean tautology() {
        if (err != null) {
            return false;
        }
        for (MessageEvaluator evaluator : evaluators) {
            if (!evaluator.tautology()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ValidationResult evaluate(JavaValue val, boolean failFast) {
        return evaluateMessage(val.value(), failFast);
    }

    @Override
    public void append(Evaluator eval) {
        if (eval instanceof MessageEvaluator) {
            append((MessageEvaluator) eval);
        }
    }

    @Override
    public void append(MessageEvaluator eval) {
        evaluators.add(eval);
    }

    @Override
    public ValidationResult evaluateMessage(Message val, boolean failFast) throws ValidationError {
        for (MessageEvaluator evaluator : evaluators) {
            ValidationResult validationResult = evaluator.evaluateMessage(val, failFast);
            if (validationResult.isFailure()) {
                return validationResult;
            }
        }
        return ValidationResult.success();
    }
}
