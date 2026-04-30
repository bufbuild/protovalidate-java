// Copyright 2023-2026 Buf Technologies, Inc.
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

package build.buf.protovalidate.rules;

import build.buf.protovalidate.FieldPathUtils;
import build.buf.protovalidate.ValueEvaluator;
import build.buf.validate.FieldPath;
import build.buf.validate.FieldPathElement;
import com.google.protobuf.Descriptors.FieldDescriptor;
import java.util.Collections;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Common context shared across native rule evaluators: the field's descriptor, its single
 * containing-message field path element, and any nested-rule prefix that must be prepended to rule
 * paths in violations.
 *
 * <p>Mirrors the {@code base} struct in protovalidate-go's {@code base.go}, adapted to Java's
 * existing pattern of letting violations bubble up the call stack with prepended path elements (see
 * {@link FieldPathUtils#updatePaths}).
 */
final class RuleBase {
  private static final List<FieldPathElement> EMPTY_PREFIX = Collections.emptyList();

  private final @Nullable FieldDescriptor descriptor;
  private final @Nullable FieldPathElement fieldPathElement;
  private final @Nullable FieldPath rulePrefix;

  private RuleBase(
      @Nullable FieldDescriptor descriptor,
      @Nullable FieldPathElement fieldPathElement,
      @Nullable FieldPath rulePrefix) {
    this.descriptor = descriptor;
    this.fieldPathElement = fieldPathElement;
    this.rulePrefix = rulePrefix;
  }

  /**
   * Builds a {@link RuleBase} from the given {@link ValueEvaluator}, computing the field path
   * element from its descriptor and capturing its nested-rule prefix.
   */
  static RuleBase of(ValueEvaluator valueEvaluator) {
    FieldDescriptor desc = valueEvaluator.getDescriptor();
    FieldPathElement fpe = (desc != null) ? FieldPathUtils.fieldPathElement(desc) : null;
    return new RuleBase(desc, fpe, valueEvaluator.getNestedRule());
  }

  /** The descriptor of the field being validated, or null when validating a non-field value. */
  @Nullable FieldDescriptor getDescriptor() {
    return descriptor;
  }

  /**
   * The {@link FieldPathElement} for prepending to violation field paths, or null when there is no
   * field context (e.g. the value being validated is not a message field).
   */
  @Nullable FieldPathElement getFieldPathElement() {
    return fieldPathElement;
  }

  /**
   * The nested-rule path elements (e.g. {@code repeated.items}, {@code map.keys}) to prepend to
   * violation rule paths. Empty when there is no nested-rule context.
   */
  List<FieldPathElement> getRulePrefixElements() {
    if (rulePrefix == null) {
      return EMPTY_PREFIX;
    }
    return rulePrefix.getElementsList();
  }
}
