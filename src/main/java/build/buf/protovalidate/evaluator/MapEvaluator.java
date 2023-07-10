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

import build.buf.gen.buf.validate.FieldConstraints;
import build.buf.gen.buf.validate.MapRules;
import build.buf.protovalidate.results.ExecutionException;
import build.buf.protovalidate.results.ValidationResult;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;

import java.util.Map;

/**
 * Performs validation on a map field's KV Pairs.
 */
class MapEvaluator implements Evaluator {
    /**
     * KeyConstraints are checked on the map keys
     */
    private final ValueEvaluator keyEvaluator;
    /**
     * ValueConstraints are checked on the map values
     */
    private final ValueEvaluator valueEvaluator;

    /**
     * Constructs a MapEvaluator
     */
    MapEvaluator(FieldConstraints fieldConstraints, Descriptors.FieldDescriptor fieldDescriptor) {
        MapRules map = fieldConstraints.getMap();
        this.keyEvaluator = new ValueEvaluator(map.getKeys(), fieldDescriptor.getMessageType().findFieldByNumber(1));
        this.valueEvaluator = new ValueEvaluator(map.getValues(), fieldDescriptor.getMessageType().findFieldByNumber(2));
    }

    public ValueEvaluator getKeyEvaluator() {
        return keyEvaluator;
    }

    public ValueEvaluator getValueEvaluator() {
        return valueEvaluator;
    }

    @Override
    public boolean tautology() {
        return keyEvaluator.tautology() &&
                valueEvaluator.tautology();
    }

    @Override
    public ValidationResult evaluate(Value val, boolean failFast) throws ExecutionException {
        ValidationResult validationResult = new ValidationResult();
        Map<Value, Value> mapValue = val.mapValue();
        for (Map.Entry<Value, Value> entry : mapValue.entrySet()) {
            ValidationResult evalResult = evalPairs(entry.getKey(), entry.getValue(), failFast);
            if (!validationResult.merge(evalResult, failFast)) {
                return validationResult;
            }
        }
        return validationResult;
    }

    private ValidationResult evalPairs(Value key, Value value, boolean failFast) {
        ValidationResult evalResult = new ValidationResult();

        try {
            ValidationResult keyEvalResult = keyEvaluator.evaluate(key, failFast);
            if (!evalResult.merge(keyEvalResult, failFast)) {
                return keyEvalResult;
            }
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

        try {
            ValidationResult valueEvalResult = valueEvaluator.evaluate(value, failFast);
            if (!evalResult.merge(valueEvalResult, failFast)) {
                return valueEvalResult;
            }
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

        Object keyName = key.value();
        if (keyName instanceof Number) {
            evalResult.prefixErrorPaths("[%s]", keyName);
        } else {
            evalResult.prefixErrorPaths("[\"%s\"]", keyName);
        }
        return evalResult;
    }

    @Override
    public void append(Evaluator eval) {
        throw new UnsupportedOperationException("append not supported for MapEvaluator");
    }
}
