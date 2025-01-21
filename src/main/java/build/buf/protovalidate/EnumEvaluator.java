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

import build.buf.protovalidate.exceptions.ExecutionException;
import build.buf.validate.EnumRules;
import build.buf.validate.FieldConstraints;
import build.buf.validate.FieldPath;
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
  private final ConstraintViolationHelper helper;

  /** Captures all the defined values for this enum */
  private final Set<Integer> values;

  private static final Descriptors.FieldDescriptor DEFINED_ONLY_DESCRIPTOR =
      EnumRules.getDescriptor().findFieldByNumber(EnumRules.DEFINED_ONLY_FIELD_NUMBER);

  private static final FieldPath DEFINED_ONLY_RULE_PATH =
      FieldPath.newBuilder()
          .addElements(
              FieldPathUtils.fieldPathElement(
                  FieldConstraints.getDescriptor()
                      .findFieldByNumber(FieldConstraints.ENUM_FIELD_NUMBER)))
          .addElements(FieldPathUtils.fieldPathElement(DEFINED_ONLY_DESCRIPTOR))
          .build();

  /**
   * Constructs a new evaluator for enum values.
   *
   * @param valueDescriptors the list of {@link Descriptors.EnumValueDescriptor} for the enum.
   */
  EnumEvaluator(
      ValueEvaluator valueEvaluator, List<Descriptors.EnumValueDescriptor> valueDescriptors) {
    this.helper = new ConstraintViolationHelper(valueEvaluator);
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

  /**
   * Evaluates an enum value.
   *
   * @param val the value to evaluate.
   * @param failFast indicates if the evaluation should stop on the first violation.
   * @return the {@link ValidationResult} of the evaluation.
   * @throws ExecutionException if an error occurs during the evaluation.
   */
  @Override
  public List<ConstraintViolation.Builder> evaluate(Value val, boolean failFast)
      throws ExecutionException {
    Descriptors.EnumValueDescriptor enumValue = val.value(Descriptors.EnumValueDescriptor.class);
    if (enumValue == null) {
      return ConstraintViolation.NO_VIOLATIONS;
    }
    if (!values.contains(enumValue.getNumber())) {
      return Collections.singletonList(
          ConstraintViolation.newBuilder()
              .addAllRulePathElements(helper.getRulePrefixElements())
              .addAllRulePathElements(DEFINED_ONLY_RULE_PATH.getElementsList())
              .addFirstFieldPathElement(helper.getFieldPathElement())
              .setConstraintId("enum.defined_only")
              .setMessage("value must be one of the defined enum values")
              .setFieldValue(new ConstraintViolation.FieldValue(val))
              .setRuleValue(new ConstraintViolation.FieldValue(true, DEFINED_ONLY_DESCRIPTOR)));
    }
    return ConstraintViolation.NO_VIOLATIONS;
  }
}
