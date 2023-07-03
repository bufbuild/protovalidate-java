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
import build.buf.protovalidate.errors.ExceptionUtils;
import build.buf.protovalidate.errors.ValidationError;

import java.util.Map;

public class KvPairs implements Evaluator {

    // KeyConstraints are checked on the map keys
    public final Value keyConstraints;
    // ValueConstraints are checked on the map values
    public final Value valueConstraints;

    public KvPairs() {
        this(new Value(), new Value());
    }

    public KvPairs(Value keyConstraints, Value valueConstraints) {
        this.keyConstraints = keyConstraints;
        this.valueConstraints = valueConstraints;
    }

    @Override
    public boolean tautology() {
        return keyConstraints.tautology() &&
                valueConstraints.tautology();
    }

    @Override
    public ValidationResult evaluate(JavaValue val, boolean failFast) {
        Map<JavaValue, JavaValue> mapEntries = val.mapValue();
        ValidationError error = new ValidationError();
        for (Map.Entry<JavaValue, JavaValue> entry : mapEntries.entrySet()) {
            Exception evalErr = evalPairs(entry.getKey(), entry.getValue(), failFast);
            if (evalErr != null) {
                String keyName = entry.getKey().value().toString();
                if (entry.getKey().value() instanceof Number) {
                    ExceptionUtils.prefixErrorPaths(evalErr, "[%s]", keyName);
                } else {
                    ExceptionUtils.prefixErrorPaths(evalErr, "[\"%s\"]", keyName);
                }
                boolean merged = ExceptionUtils.merge(error, evalErr, failFast);
                if (!merged) {
                    return new ValidationResult(error);
                }
            }
        }
        if (!error.isEmpty()) {
            return new ValidationResult(error);
        }
        return ValidationResult.success();
    }

    private ValidationError evalPairs(JavaValue key, JavaValue value, boolean failFast) {
        ValidationError error = new ValidationError();
        ValidationResult keyEvalErr = keyConstraints.evaluate(key, failFast);
        if (keyEvalErr.isFailure()) {
            ExceptionUtils.merge(error, keyEvalErr.error(), failFast);
            if (!error.isEmpty()) {
                return error;
            }
        }
        ValidationResult valueEvalErr = valueConstraints.evaluate(value, failFast);
        if (valueEvalErr.isFailure()) {
            ExceptionUtils.merge(error, valueEvalErr.error(), failFast);
            if (!error.isEmpty()) {
                return error;
            }
        }
        if (!error.isEmpty()) {
            return error;
        }
        return null;
    }


    @Override
    public void append(Evaluator eval) {
        throw new UnsupportedOperationException("append not supported for KvPairs");
    }
}
