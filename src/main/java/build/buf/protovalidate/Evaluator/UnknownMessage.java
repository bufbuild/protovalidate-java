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
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.MessageOrBuilder;

// unknownMessage is a MessageEvaluator for an unknown descriptor. This is
// returned only if lazy-building of evaluators has been disabled and an unknown
// descriptor is encountered.
public class UnknownMessage implements MessageEvaluator {
    private Descriptor desc;

    public UnknownMessage(Descriptor desc) {
        this.desc = desc;
    }

    public Exception err() {
        return new Exception(
                String.format("no evaluator available for %s", this.desc.getFullName()));
    }

    @Override
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
