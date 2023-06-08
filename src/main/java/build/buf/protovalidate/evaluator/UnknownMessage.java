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
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;

// unknownMessage is a MessageEvaluator for an unknown descriptor. This is
// returned only if lazy-building of evaluators has been disabled and an unknown
// descriptor is encountered.
public class UnknownMessage implements MessageEvaluator {
    private final Descriptor desc;

    public UnknownMessage(Descriptor desc) {
        this.desc = desc;
    }

    public ValidationError err() {
        throw new ValidationError("No evaluator available for " + desc.getFullName());
    }

    @Override
    public boolean tautology() {
        return false;
    }

    @Override
    public void evaluate(DynamicMessage val, boolean failFast) throws ValidationError {
        throw this.err();
    }

    @Override
    public void evaluateMessage(Message val, boolean failFast) throws ValidationError {
        throw this.err();
    }
}
