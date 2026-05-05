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

package build.buf.protovalidate;

import build.buf.validate.FieldPathElement;
import com.google.protobuf.Descriptors.FieldDescriptor;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A pre-built bundle for a single rule site: the two-element rule-path suffix ({@code
 * [FieldRules.<type>, <Type>Rules.<rule>]}), the leaf rule descriptor, and an optional constant
 * rule id and message.
 *
 * <p>Each native rule type instantiates one {@link RuleSite} per supported rule at class init time
 * so violation construction at validation time only allocates the violation builder itself, not the
 * path-element protos. Mirrors the {@code ruleSite} struct in protovalidate-go's {@code base.go}.
 */
final class RuleSite {
  private final List<FieldPathElement> pathElements;
  private final FieldDescriptor leafDescriptor;
  private final @Nullable String ruleId;
  private final @Nullable String message;

  private RuleSite(
      List<FieldPathElement> pathElements,
      FieldDescriptor leafDescriptor,
      @Nullable String ruleId,
      @Nullable String message) {
    this.pathElements = pathElements;
    this.leafDescriptor = leafDescriptor;
    this.ruleId = ruleId;
    this.message = message;
  }

  /**
   * Builds a {@link RuleSite} from a rule-type field descriptor (e.g. {@code FieldRules.string})
   * and a leaf rule field descriptor (e.g. {@code StringRules.min_len}).
   *
   * @param ruleTypeDescriptor descriptor of the {@code FieldRules} oneof case (e.g. the {@code
   *     string} field on {@code FieldRules}).
   * @param leafDescriptor descriptor of the specific rule (e.g. {@code min_len}).
   * @param ruleId optional constant rule id for this site (e.g. {@code "string.min_len"}); may be
   *     null when the rule id is computed per violation (e.g. well-known formats with empty/error
   *     variants).
   * @param message optional constant violation message; may be null when the message is built per
   *     violation from the failing value.
   */
  static RuleSite of(
      FieldDescriptor ruleTypeDescriptor,
      FieldDescriptor leafDescriptor,
      @Nullable String ruleId,
      @Nullable String message) {
    List<FieldPathElement> elements =
        Collections.unmodifiableList(
            Arrays.asList(
                FieldPathUtils.fieldPathElement(ruleTypeDescriptor),
                FieldPathUtils.fieldPathElement(leafDescriptor)));
    return new RuleSite(elements, leafDescriptor, ruleId, message);
  }

  List<FieldPathElement> getPathElements() {
    return pathElements;
  }

  FieldDescriptor getLeafDescriptor() {
    return leafDescriptor;
  }

  @Nullable String getRuleId() {
    return ruleId;
  }

  @Nullable String getMessage() {
    return message;
  }
}
