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
import com.google.protobuf.DynamicMessage;

public class Value implements Evaluator {
    // Zero is the default or zero-value for this value's type
    // TODO: not a message
    public final com.google.protobuf.Value zero;
    // Constraints are the individual evaluators applied to a value
    public final Evaluators constraints;
    // IgnoreEmpty indicates that the Constraints should not be applied if the
    // field is unset or the default (typically zero) value.
    public final boolean ignoreEmpty;

    public Value() {
        this(null, false);
    }
    public Value(com.google.protobuf.Value zero, boolean ignoreEmpty) {
        this.zero = zero;
        this.ignoreEmpty = ignoreEmpty;
        this.constraints = new Evaluators(null);
    }

    @Override
    public boolean tautology() {
        return constraints.evaluators.isEmpty();
    }

    @Override
    public ValidationResult evaluate(DynamicMessage val, boolean failFast) {
        if (ignoreEmpty && val.equals(zero)) {
            return ValidationResult.success();
        }
        for (Evaluator constraint : constraints.evaluators) {
            ValidationResult validationResult = constraint.evaluate(val, failFast);
            if (validationResult.isFailure()) {
                return validationResult;
            }
        }
        return ValidationResult.success();
    }

    public void append(Evaluator eval) {
        if (eval != null && !eval.tautology()) {
            this.constraints.append(eval);
        }
    }
}