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

package build.buf.protovalidate.internal.evaluator;

import build.buf.gen.buf.validate.Violation;
import build.buf.protovalidate.ValidationResult;
import build.buf.protovalidate.exceptions.ExecutionException;
import com.google.protobuf.Descriptors;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@link EnumEvaluator} checks an enum value being a member of the defined values exclusively. This
 * check is handled outside CEL as enums are completely type erased to integers.
 */
class EnumEvaluator implements Evaluator {
  /** Captures all the defined values for this enum */
  private final Set<Integer> values;

  /** Constructs a new evaluator for enum values. */
  EnumEvaluator(List<Descriptors.EnumValueDescriptor> valueDescriptors) {
    if (valueDescriptors.isEmpty()) {
      this.values = Collections.emptySet();
    } else {
      this.values =
          valueDescriptors.stream()
              .map(Descriptors.EnumValueDescriptor::getNumber)
              .collect(Collectors.toSet());
    }
  }

  @Override
  public boolean tautology() {
    return false;
  }

  @Override
  public ValidationResult evaluate(Value val, boolean failFast) throws ExecutionException {
    Descriptors.EnumValueDescriptor enumValue = val.value(Descriptors.EnumValueDescriptor.class);
    if (!values.contains(enumValue.getNumber())) {
      return new ValidationResult(
          Collections.singletonList(
              Violation.newBuilder()
                  .setConstraintId("enum.defined_only")
                  .setMessage("value must be one of the defined enum values")
                  .build()));
    }
    return ValidationResult.EMPTY;
  }
}
