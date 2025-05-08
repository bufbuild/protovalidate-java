// Copyright 2023-2024 Buf Technologies, Inc.
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

import build.buf.protovalidate.exceptions.CompilationException;
import build.buf.protovalidate.exceptions.ExecutionException;
import build.buf.protovalidate.exceptions.ValidationException;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Message;
import java.util.ArrayList;
import java.util.List;
import org.projectnessie.cel.Env;
import org.projectnessie.cel.Library;

class ValidatorImpl implements Validator {
  /** evaluatorBuilder is the builder used to construct the evaluator for a given message. */
  private final EvaluatorBuilder evaluatorBuilder;

  /**
   * failFast indicates whether the validator should stop evaluating rules after the first
   * violation.
   */
  private final boolean failFast;

  ValidatorImpl(Config config) {
    Env env = Env.newEnv(Library.Lib(new ValidateLibrary()));
    this.evaluatorBuilder = new EvaluatorBuilder(env, config);
    this.failFast = config.isFailFast();
  }

  /**
   * Checks that message satisfies its rules. Rules are defined within the Protobuf file as options
   * from the buf.validate package. A {@link ValidationResult} is returned which contains a list of
   * violations. If the list is empty, the message is valid. If the list is non-empty, the message
   * is invalid. An exception is thrown if the message cannot be validated because the evaluation
   * logic for the message cannot be built ({@link CompilationException}), or there is a type error
   * when attempting to evaluate a CEL expression associated with the message ({@link
   * ExecutionException}).
   *
   * @param msg the {@link Message} to be validated.
   * @return the {@link ValidationResult} from the evaluation.
   * @throws ValidationException if there are any compilation or validation execution errors.
   */
  @Override
  public ValidationResult validate(Message msg) throws ValidationException {
    if (msg == null) {
      return ValidationResult.EMPTY;
    }
    Descriptor descriptor = msg.getDescriptorForType();
    Evaluator evaluator = evaluatorBuilder.load(descriptor);
    List<RuleViolation.Builder> result = evaluator.evaluate(new MessageValue(msg), this.failFast);
    if (result.isEmpty()) {
      return ValidationResult.EMPTY;
    }
    List<Violation> violations = new ArrayList<>(result.size());
    for (RuleViolation.Builder builder : result) {
      violations.add(builder.build());
    }
    return new ValidationResult(violations);
  }
}
