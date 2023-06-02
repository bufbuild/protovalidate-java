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
import com.google.protobuf.Message;
import lombok.Data;

@Data
public class Value implements Evaluator {
    // Zero is the default or zero-value for this value's type
    // TODO: not a message
    private Object zero;
    // Constraints are the individual evaluators applied to a value
    private Evaluators constraints;
    // IgnoreEmpty indicates that the Constraints should not be applied if the
    // field is unset or the default (typically zero) value.
    private boolean ignoreEmpty;

    public Value(Object zero, boolean ignoreEmpty) {
        this.zero = zero;
        this.ignoreEmpty = ignoreEmpty;
        this.constraints = new Evaluators(null);
    }

    @Override
    public boolean tautology() {
        return false;
    }

    @Override
    public void evaluate(DynamicMessage val, boolean failFast) throws ValidationError {

    }

    public void append(MessageEvaluator eval) {
        if (eval != null && !eval.tautology()) {
            this.constraints.append(eval);
        }
    }
}
