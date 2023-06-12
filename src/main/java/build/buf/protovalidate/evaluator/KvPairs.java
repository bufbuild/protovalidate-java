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
        return false;
    }

    @Override
    public ValidationResult evaluate(DynamicMessage val, boolean failFast) {
        return ValidationResult.success();
    }

    @Override
    public void append(Evaluator eval) {
        throw new UnsupportedOperationException("append not supported for KvPairs");
    }
}
