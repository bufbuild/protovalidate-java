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

package build.buf.protovalidate.internal.errors;

import build.buf.validate.FieldPath;
import build.buf.validate.FieldPathElement;
import build.buf.validate.Violation;
import com.google.protobuf.Descriptors;
import java.util.ArrayList;
import java.util.List;

/** Utility class for manipulating error paths in violations. */
public final class FieldPathUtils {
  private FieldPathUtils() {}

  /**
   * Prepends the field paths of the given violations with the provided element.
   *
   * @param violations The list of violations to operate on.
   * @param element The element to prefix to each field path.
   * @param skipSubscript Skips prepending a field path if the first element has a subscript.
   * @return The modified violations with prepended field paths.
   */
  public static List<Violation> prependFieldPaths(
      List<Violation> violations, FieldPathElement element, boolean skipSubscript) {
    List<Violation> result = new ArrayList<>();
    for (Violation violation : violations) {
      // Special case: Here we skip prepending if the first element has a subscript. This is a weird
      // special case that makes it significantly simpler to handle reverse-constructing paths with
      // maps and repeated fields.
      if (skipSubscript
          && violation.getField().getElementsCount() > 0
          && violation.getField().getElements(0).getSubscriptCase()
              != FieldPathElement.SubscriptCase.SUBSCRIPT_NOT_SET) {
        result.add(violation);
        continue;
      }
      result.add(
          violation.toBuilder()
              .setField(
                  FieldPath.newBuilder()
                      .addElements(element)
                      .addAllElements(violation.getField().getElementsList())
                      .build())
              .build());
    }
    return result;
  }

  /**
   * Prepends the rule paths of the given violations with the provided elements.
   *
   * @param violations The list of violations to operate on.
   * @param elements The elements to prefix to each rule path.
   * @return The modified violations with prepended rule paths.
   */
  public static List<Violation> prependRulePaths(
      List<Violation> violations, Iterable<FieldPathElement> elements) {
    List<Violation> result = new ArrayList<>();
    for (Violation violation : violations) {
      result.add(
          violation.toBuilder()
              .setRule(
                  FieldPath.newBuilder()
                      .addAllElements(elements)
                      .addAllElements(violation.getRule().getElementsList())
                      .build())
              .build());
    }
    return result;
  }

  /**
   * Calculates the field path strings for each violation.
   *
   * @param violations The list of violations to operate on.
   * @return The modified violations with field path strings.
   */
  public static List<Violation> calculateFieldPathStrings(List<Violation> violations) {
    List<Violation> result = new ArrayList<>();
    for (Violation violation : violations) {
      if (violation.getField().getElementsCount() > 0) {
        result.add(
            violation.toBuilder().setFieldPath(fieldPathString(violation.getField())).build());
      } else {
        result.add(violation);
      }
    }
    return result;
  }

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
  public static FieldPathElement.Builder fieldPathElement(
      Descriptors.FieldDescriptor fieldDescriptor) {
    String name;
    if (fieldDescriptor.isExtension()) {
      name = "[" + fieldDescriptor.getFullName() + "]";
    } else {
      name = fieldDescriptor.getName();
    }
    return FieldPathElement.newBuilder()
        .setFieldNumber(fieldDescriptor.getNumber())
        .setFieldName(name)
        .setFieldType(fieldDescriptor.getType().toProto());
  }
}
