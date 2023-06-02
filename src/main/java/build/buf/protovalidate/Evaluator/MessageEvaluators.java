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

package build.buf.protovalidate.Evaluator;

import build.buf.protovalidate.Errors.ValidationError;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.MessageOrBuilder;

import java.util.List;

class MessageEvaluators implements MessageEvaluator {
    private List<MessageEvaluator> messageEvaluators;

    public MessageEvaluators(List<MessageEvaluator> messageEvaluators) {
        this.messageEvaluators = messageEvaluators;
    }

    @Override
    public boolean tautology() {
        for (MessageEvaluator eval : messageEvaluators) {
            if (!eval.tautology()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void evaluate(DynamicMessage val, boolean failFast) throws ValidationError {
        evaluateMessage(val, failFast);
    }

    @Override
    public void evaluateMessage(MessageOrBuilder val, boolean failFast) throws ValidationError {
    }

    public void append(MessageEvaluator eval) {
        if (eval != null && !eval.tautology()) {
            this.messageEvaluators.add(eval);
        }
    }
}