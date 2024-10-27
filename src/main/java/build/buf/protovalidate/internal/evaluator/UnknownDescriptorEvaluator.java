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

package build.buf.protovalidate.internal.evaluator;

import build.buf.protovalidate.ValidationResult;
import build.buf.protovalidate.Value;
import build.buf.protovalidate.exceptions.ExecutionException;
import build.buf.validate.Violation;
import com.google.protobuf.Descriptors.Descriptor;
import java.util.Collections;

/**
 * An {@link Evaluator} for an unknown descriptor. This is returned only if lazy-building of
 * evaluators has been disabled and an unknown descriptor is encountered.
 */
class UnknownDescriptorEvaluator implements Evaluator {
  /** The descriptor targeted by this evaluator. */
  private final Descriptor desc;

  /** Constructs a new {@link UnknownDescriptorEvaluator}. */
  UnknownDescriptorEvaluator(Descriptor desc) {
    this.desc = desc;
  }

  @Override
  public boolean tautology() {
    return false;
  }

  @Override
  public ValidationResult evaluate(Value val, boolean failFast) throws ExecutionException {
    return new ValidationResult(
        Collections.singletonList(
            Violation.newBuilder()
                .setMessage("No evaluator available for " + desc.getFullName())
                .build()));
  }
}
