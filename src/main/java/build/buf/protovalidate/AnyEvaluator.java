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

import build.buf.validate.AnyRules;
import build.buf.validate.FieldConstraints;
import build.buf.validate.FieldPath;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A specialized evaluator for applying {@link build.buf.validate.AnyRules} to an {@link
 * com.google.protobuf.Any} message. This is handled outside CEL which attempts to hydrate {@link
 * com.google.protobuf.Any}'s within an expression, breaking evaluation if the type is unknown at
 * runtime.
 */
class AnyEvaluator implements Evaluator {
  private final ConstraintViolationHelper helper;
  private final Descriptors.FieldDescriptor typeURLDescriptor;
  private final Set<String> in;
  private final List<String> inValue;
  private final Set<String> notIn;
  private final List<String> notInValue;

  private static final Descriptors.FieldDescriptor ANY_DESCRIPTOR =
      FieldConstraints.getDescriptor().findFieldByNumber(FieldConstraints.ANY_FIELD_NUMBER);

  private static final Descriptors.FieldDescriptor IN_DESCRIPTOR =
      AnyRules.getDescriptor().findFieldByNumber(AnyRules.IN_FIELD_NUMBER);

  private static final Descriptors.FieldDescriptor NOT_IN_DESCRIPTOR =
      AnyRules.getDescriptor().findFieldByNumber(AnyRules.NOT_IN_FIELD_NUMBER);

  private static final FieldPath IN_RULE_PATH =
      FieldPath.newBuilder()
          .addElements(FieldPathUtils.fieldPathElement(ANY_DESCRIPTOR))
          .addElements(FieldPathUtils.fieldPathElement(IN_DESCRIPTOR))
          .build();

  private static final FieldPath NOT_IN_RULE_PATH =
      FieldPath.newBuilder()
          .addElements(FieldPathUtils.fieldPathElement(ANY_DESCRIPTOR))
          .addElements(FieldPathUtils.fieldPathElement(NOT_IN_DESCRIPTOR))
          .build();

  /** Constructs a new evaluator for {@link build.buf.validate.AnyRules} messages. */
  AnyEvaluator(
      ValueEvaluator valueEvaluator,
      Descriptors.FieldDescriptor typeURLDescriptor,
      List<String> in,
      List<String> notIn) {
    this.helper = new ConstraintViolationHelper(valueEvaluator);
    this.typeURLDescriptor = typeURLDescriptor;
    this.in = stringsToSet(in);
    this.inValue = in;
    this.notIn = stringsToSet(notIn);
    this.notInValue = notIn;
  }

  @Override
  public List<ConstraintViolation.Builder> evaluate(Value val, boolean failFast)
      throws ExecutionException {
    Message anyValue = val.messageValue();
    if (anyValue == null) {
      return ConstraintViolation.NO_VIOLATIONS;
    }
    List<ConstraintViolation.Builder> violationList = new ArrayList<>();
    String typeURL = (String) anyValue.getField(typeURLDescriptor);
    if (!in.isEmpty() && !in.contains(typeURL)) {
      ConstraintViolation.Builder violation =
          ConstraintViolation.newBuilder()
              .addAllRulePathElements(helper.getRulePrefixElements())
              .addAllRulePathElements(IN_RULE_PATH.getElementsList())
              .addFirstFieldPathElement(helper.getFieldPathElement())
              .setConstraintId("any.in")
              .setMessage("type URL must be in the allow list")
              .setFieldValue(new ConstraintViolation.FieldValue(val))
              .setRuleValue(new ConstraintViolation.FieldValue(this.inValue, IN_DESCRIPTOR));
      violationList.add(violation);
      if (failFast) {
        return violationList;
      }
    }
    if (!notIn.isEmpty() && notIn.contains(typeURL)) {
      ConstraintViolation.Builder violation =
          ConstraintViolation.newBuilder()
              .addAllRulePathElements(helper.getRulePrefixElements())
              .addAllRulePathElements(NOT_IN_RULE_PATH.getElementsList())
              .addFirstFieldPathElement(helper.getFieldPathElement())
              .setConstraintId("any.not_in")
              .setMessage("type URL must not be in the block list")
              .setFieldValue(new ConstraintViolation.FieldValue(val))
              .setRuleValue(new ConstraintViolation.FieldValue(this.notInValue, NOT_IN_DESCRIPTOR));
      violationList.add(violation);
    }
    return violationList;
  }

  @Override
  public boolean tautology() {
    return in.isEmpty() && notIn.isEmpty();
  }

  /** stringsToMap converts a string list to a set for fast lookup. */
  private static Set<String> stringsToSet(List<String> strings) {
    if (strings.isEmpty()) {
      return Collections.emptySet();
    }
    return new HashSet<>(strings);
  }
}
