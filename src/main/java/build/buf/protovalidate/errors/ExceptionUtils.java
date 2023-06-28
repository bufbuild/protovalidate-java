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

package build.buf.protovalidate.errors;

import build.buf.validate.Violation;

public class ExceptionUtils {

    public static boolean merge(Exception dst, Exception src, boolean failFast) {
        if (src == null) {
            return true;
        }

        if (src instanceof ValidationError) {
            ValidationError srcValErrs = (ValidationError) src;
            if (dst == null) {
                return !(failFast && srcValErrs.getViolationsCount() > 0);
            }

            if (dst instanceof ValidationError) {
                ValidationError dstValErrs = (ValidationError) dst;
                dstValErrs.violations.addAll(srcValErrs.violations);
                return !(failFast && dstValErrs.getViolationsCount() > 0);
            }
        }

        return false;
    }

    public static void prefixErrorPaths(Exception err, String format, Object... args) {
        if (err instanceof ValidationError) {
            ValidationError valErr = (ValidationError) err;
            prefixFieldPaths(valErr, format, args);
        }
    }

    private static void prefixFieldPaths(ValidationError valErr, String format, Object... args) {
        String prefix = String.format(format, args);
        for (Violation violation : valErr.violations) {
            Violation.Builder builder = violation.toBuilder();
            if (violation.getFieldPath().isEmpty()) {
                builder.setFieldPath(prefix);
            } else if (violation.getFieldPath().charAt(0) == '[') {
                builder.setFieldPath(prefix + violation.getFieldPath());
            } else {
                builder.setFieldPath(prefix + "." + violation.getFieldPath());
            }
            valErr.violations.remove(violation);
            valErr.violations.add(builder.build());
        }
    }
}
