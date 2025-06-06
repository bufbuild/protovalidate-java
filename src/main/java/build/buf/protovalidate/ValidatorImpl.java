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
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Message;
import dev.cel.bundle.Cel;
import dev.cel.bundle.CelFactory;
import java.util.ArrayList;
import java.util.List;

class ValidatorImpl implements Validator {
  /** evaluatorBuilder is the builder used to construct the evaluator for a given message. */
  private final EvaluatorBuilder evaluatorBuilder;

  /**
   * failFast indicates whether the validator should stop evaluating rules after the first
   * violation.
   */
  private final boolean failFast;

  ValidatorImpl(Config config) {
    ValidateLibrary validateLibrary = new ValidateLibrary();
    Cel cel =
        CelFactory.standardCelBuilder()
            .addCompilerLibraries(validateLibrary)
            .addRuntimeLibraries(validateLibrary)
            .build();
    this.evaluatorBuilder = new EvaluatorBuilder(cel, config);
    this.failFast = config.isFailFast();
  }

  ValidatorImpl(Config config, List<Descriptor> descriptors, boolean disableLazy)
      throws CompilationException {
    ValidateLibrary validateLibrary = new ValidateLibrary();
    Cel cel =
        CelFactory.standardCelBuilder()
            .addCompilerLibraries(validateLibrary)
            .addRuntimeLibraries(validateLibrary)
            .build();
    this.evaluatorBuilder = new EvaluatorBuilder(cel, config, descriptors, disableLazy);
    this.failFast = config.isFailFast();
  }

  /** {@inheritDoc} */
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
