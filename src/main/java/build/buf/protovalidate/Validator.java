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
import build.buf.protovalidate.exceptions.ValidationException;
import build.buf.protovalidate.internal.celext.ValidateLibrary;
import build.buf.protovalidate.internal.errors.ConstraintViolation;
import build.buf.protovalidate.internal.evaluator.Evaluator;
import build.buf.protovalidate.internal.evaluator.EvaluatorBuilder;
import build.buf.protovalidate.internal.evaluator.MessageValue;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Message;
import java.util.ArrayList;
import java.util.List;
import org.projectnessie.cel.Env;
import org.projectnessie.cel.Library;

/** Performs validation on any proto.Message values. The Validator is safe for concurrent use. */
public class Validator {
  /** evaluatorBuilder is the builder used to construct the evaluator for a given message. */
  private final EvaluatorBuilder evaluatorBuilder;

  /**
   * failFast indicates whether the validator should stop evaluating constraints after the first
   * violation.
   */
  private final boolean failFast;

  /**
   * Constructs a new {@link Validator}.
   *
   * @param config specified configuration.
   */
  public Validator(Config config) {
    Env env = Env.newEnv(Library.Lib(new ValidateLibrary()));
    this.evaluatorBuilder = new EvaluatorBuilder(env, config);
    this.failFast = config.isFailFast();
  }

  /** Constructs a new {@link Validator} with a default configuration. */
  public Validator() {
    Config config = Config.newBuilder().build();
    Env env = Env.newEnv(Library.Lib(new ValidateLibrary()));
    this.evaluatorBuilder = new EvaluatorBuilder(env, config);
    this.failFast = config.isFailFast();
  }

  /**
   * Checks that message satisfies its constraints. Constraints are defined within the Protobuf file
   * as options from the buf.validate package. A {@link ValidationResult} is returned which contains
   * a list of violations. If the list is empty, the message is valid. If the list is non-empty, the
   * message is invalid. An exception is thrown if the message cannot be validated because the
   * evaluation logic for the message cannot be built ({@link CompilationException}), or there is a
   * type error when attempting to evaluate a CEL expression associated with the message ({@link
   * build.buf.protovalidate.exceptions.ExecutionException}).
   *
   * @param msg the {@link Message} to be validated.
   * @return the {@link ValidationResult} from the evaluation.
   * @throws ValidationException if there are any compilation or validation execution errors.
   */
  public ValidationResult validate(Message msg) throws ValidationException {
    if (msg == null) {
      return ValidationResult.EMPTY;
    }
    Descriptor descriptor = msg.getDescriptorForType();
    Evaluator evaluator = evaluatorBuilder.load(descriptor);
    List<ConstraintViolation.Builder> result = evaluator.evaluate(new MessageValue(msg), failFast);
    if (result.isEmpty()) {
      return ValidationResult.EMPTY;
    }
    List<Violation> violations = new ArrayList<>(result.size());
    for (ConstraintViolation.Builder builder : result) {
      violations.add(builder.build());
    }
    return new ValidationResult(violations);
  }

  /**
   * Loads messages that are expected to be validated, allowing the {@link Validator} to warm up.
   * Messages included transitively (i.e., fields with message values) are automatically handled.
   *
   * @param messages the list of {@link Message} to load.
   * @throws CompilationException if there are any compilation errors during warm-up.
   */
  public void loadMessages(Message... messages) throws CompilationException {
    for (Message message : messages) {
      this.evaluatorBuilder.load(message.getDescriptorForType());
    }
  }

  /**
   * Loads message descriptors that are expected to be validated, allowing the {@link Validator} to
   * warm up. Messages included transitively (i.e., fields with message values) are automatically
   * handled.
   *
   * @param descriptors the list of {@link Descriptor} to load.
   * @throws CompilationException if there are any compilation errors during warm-up.
   */
  public void loadDescriptors(Descriptor... descriptors) throws CompilationException {
    for (Descriptor descriptor : descriptors) {
      this.evaluatorBuilder.load(descriptor);
    }
  }
}
