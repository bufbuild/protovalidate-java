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

import build.buf.validate.Violation;
import com.google.common.base.Strings;
import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import java.util.List;
import java.util.stream.Collectors;

/** Utility class for manipulating error paths in violations. */
final class ErrorPathUtils {
  private ErrorPathUtils() {}

  /**
   * Prefixes the error paths of the given violations with a format string and its arguments.
   *
   * @param violations The list of violations to modify.
   * @param format The format string to use as the prefix.
   * @param args The arguments to apply to the format string.
   * @return The modified list of violations with prefixed error paths.
   */
  @FormatMethod
  static List<Violation> prefixErrorPaths(
      List<Violation> violations, @FormatString String format, Object... args) {
    String prefix = String.format(format, args);
    return violations.stream()
        .map(
            violation -> {
              String fieldPath = violation.getFieldPath();
              String prefixedFieldPath;
              if (fieldPath.isEmpty()) {
                prefixedFieldPath = prefix;
              } else if (fieldPath.charAt(0) == '[') {
                prefixedFieldPath = prefix + fieldPath;
              } else {
                prefixedFieldPath = Strings.lenientFormat("%s.%s", prefix, fieldPath);
              }
              return violation.toBuilder().setFieldPath(prefixedFieldPath).build();
            })
        .collect(Collectors.toList());
  }
}
