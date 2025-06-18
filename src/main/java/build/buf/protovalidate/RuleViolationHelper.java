// Copyright 2023-2025 Buf Technologies, Inc.
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
import java.util.Collections;
import java.util.List;
import org.jspecify.annotations.Nullable;

final class RuleViolationHelper {
  private static final List<FieldPathElement> EMPTY_PREFIX = Collections.emptyList();

  private final @Nullable FieldPath rulePrefix;

  private final @Nullable FieldPathElement fieldPathElement;

  RuleViolationHelper(@Nullable ValueEvaluator evaluator) {
    if (evaluator != null) {
      this.rulePrefix = evaluator.getNestedRule();
      if (evaluator.getDescriptor() != null) {
        this.fieldPathElement = FieldPathUtils.fieldPathElement(evaluator.getDescriptor());
      } else {
        this.fieldPathElement = null;
      }
    } else {
      this.rulePrefix = null;
      this.fieldPathElement = null;
    }
  }

  @Nullable FieldPathElement getFieldPathElement() {
    return fieldPathElement;
  }

  List<FieldPathElement> getRulePrefixElements() {
    if (rulePrefix == null) {
      return EMPTY_PREFIX;
    }
    return rulePrefix.getElementsList();
  }
}
