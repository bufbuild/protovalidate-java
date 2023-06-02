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
import build.buf.protovalidate.Expression.ProgramSet;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.MessageOrBuilder;

public class CelPrograms implements Evaluator, MessageEvaluator {
    private ProgramSet programSet;

    // assuming the equivalent of the Go `expression.ProgramSet` in Java is a List of some sort
    public CelPrograms(ProgramSet programSet) {
        this.programSet = programSet;
    }

    public boolean tautology() {
        return false;
    }

    @Override
    public void evaluate(DynamicMessage val, boolean failFast) throws ValidationError {

    }

    @Override
    public void evaluateMessage(MessageOrBuilder val, boolean failFast) throws ValidationError {

    }
}
