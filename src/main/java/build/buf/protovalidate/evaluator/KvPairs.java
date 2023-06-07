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
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Value;

public class KvPairs implements Evaluator {

    // KeyConstraints are checked on the map keys
    private Value KeyConstraints;
    // ValueConstraints are checked on the map values
    private Value ValueConstraints;

    public KvPairs(Value KeyConstraints, Value ValueConstraints) {
        this.KeyConstraints = KeyConstraints;
        this.ValueConstraints = ValueConstraints;
    }


    @Override
    public boolean tautology() {
        return false;
    }

    @Override
    public void evaluate(DynamicMessage val, boolean failFast) throws ValidationError {

    }
}
