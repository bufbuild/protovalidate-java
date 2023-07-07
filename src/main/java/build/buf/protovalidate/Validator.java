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

package build.buf.protovalidate;

import build.buf.protovalidate.celext.ValidateLibrary;
import build.buf.protovalidate.results.ValidationException;
import build.buf.protovalidate.results.ValidationResult;
import build.buf.protovalidate.evaluator.EvaluatorBuilder;
import build.buf.protovalidate.evaluator.MessageEvaluator;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Message;
import org.projectnessie.cel.Env;
import org.projectnessie.cel.Library;

public class Validator {
    private final EvaluatorBuilder evaluatorBuilder;
    private final boolean failFast;

    public Validator(Config config) {
        Env env = Env.newEnv(Library.Lib(new ValidateLibrary()));
        this.evaluatorBuilder = new EvaluatorBuilder(env, config.disableLazy);
        this.failFast = config.failFast;
    }

    public ValidationResult validate(Message msg) throws ValidationException {
        if (msg == null) {
            return new ValidationResult();
        }
        Descriptor descriptor = msg.getDescriptorForType();
        MessageEvaluator evaluator = evaluatorBuilder.load(descriptor);
        return evaluator.evaluateMessage(msg, failFast);
    }
}
