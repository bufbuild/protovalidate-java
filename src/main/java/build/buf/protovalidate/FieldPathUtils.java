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

import build.buf.validate.FieldPath;
import build.buf.validate.FieldPathElement;
import com.google.protobuf.Descriptors;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** Utility class for manipulating error paths in violations. */
final class FieldPathUtils {
  private FieldPathUtils() {}

  /**
   * Converts the provided field path to a string.
   *
   * @param fieldPath A field path to convert to a string.
   * @return The string representation of the provided field path.
   */
  static String fieldPathString(FieldPath fieldPath) {
    StringBuilder builder = new StringBuilder();
    for (FieldPathElement element : fieldPath.getElementsList()) {
      if (builder.length() > 0) {
        builder.append(".");
      }
      builder.append(element.getFieldName());
      switch (element.getSubscriptCase()) {
        case INDEX:
          builder.append("[");
          builder.append(element.getIndex());
          builder.append("]");
          break;
        case BOOL_KEY:
          if (element.getBoolKey()) {
            builder.append("[true]");
          } else {
            builder.append("[false]");
          }
          break;
        case INT_KEY:
          builder.append("[");
          builder.append(element.getIntKey());
          builder.append("]");
          break;
        case UINT_KEY:
          builder.append("[");
          builder.append(element.getUintKey());
          builder.append("]");
          break;
        case STRING_KEY:
          builder.append("[\"");
          builder.append(element.getStringKey().replace("\\", "\\\\").replace("\"", "\\\""));
          builder.append("\"]");
          break;
        case SUBSCRIPT_NOT_SET:
          break;
      }
    }
    return builder.toString();
  }

  /**
   * Returns the field path element that refers to the provided field descriptor.
   *
   * @param fieldDescriptor The field descriptor to generate a field path element for.
   * @return The field path element that corresponds to the provided field descriptor.
   */
  static FieldPathElement fieldPathElement(Descriptors.FieldDescriptor fieldDescriptor) {
    String name;
    if (fieldDescriptor.isExtension()) {
      name = "[" + fieldDescriptor.getFullName() + "]";
    } else {
      name = fieldDescriptor.getName();
    }
    return FieldPathElement.newBuilder()
        .setFieldNumber(fieldDescriptor.getNumber())
        .setFieldName(name)
        .setFieldType(fieldDescriptor.getType().toProto())
        .build();
  }

  /**
   * Provided a list of violations, adjusts it by prepending rule and field path elements.
   *
   * @param violations A list of violations.
   * @param fieldPathElement A field path element to prepend, or null.
   * @param rulePathElements Rule path elements to prepend.
   * @return For convenience, the list of violations passed into the violations parameter.
   */
  static List<RuleViolation.Builder> updatePaths(
      List<RuleViolation.Builder> violations,
      @Nullable FieldPathElement fieldPathElement,
      List<FieldPathElement> rulePathElements) {
    if (fieldPathElement != null || !rulePathElements.isEmpty()) {
      for (RuleViolation.Builder violation : violations) {
        for (int i = rulePathElements.size() - 1; i >= 0; i--) {
          violation.addFirstRulePathElement(rulePathElements.get(i));
        }
        if (fieldPathElement != null) {
          violation.addFirstFieldPathElement(fieldPathElement);
        }
      }
    }
    return violations;
  }
}
