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

import build.buf.protovalidate.Evaluator;
import build.buf.protovalidate.Internal;
import build.buf.protovalidate.ValueEvaluator;
import build.buf.validate.FieldRules;
import com.google.protobuf.Descriptors.FieldDescriptor;
import org.jspecify.annotations.Nullable;

/**
 * Entry point for native rule evaluators. {@code EvaluatorBuilder} calls {@link #tryBuild} once per
 * field; if a native evaluator covers some rules, those rules are cleared on the supplied {@code
 * FieldRules.Builder} and the residual is then handed to CEL compilation. Returns null when no
 * native evaluator applies — CEL handles the field unchanged.
 *
 * <p>The clone-and-clear contract ensures forward compatibility: when protovalidate adds a new rule
 * that this codebase hasn't yet implemented natively, the rule remains on the residual {@code
 * FieldRules} and CEL enforces it. Native rules are an optimization, not a replacement.
 */
@Internal
public final class Rules {
  private Rules() {}

  /**
   * Attempts to build a native evaluator for the standard rules on {@code fieldDescriptor}.
   *
   * <p>Any rule covered natively is cleared on {@code rulesBuilder} so that {@code RuleCache}
   * compiles CEL programs only for rules left untouched. The caller is expected to pass a builder
   * it owns (typically obtained via {@code fieldRules.toBuilder()} on a clone) and to call {@code
   * build()} on the residual before handing it to {@code RuleCache.compile}.
   *
   * @param fieldDescriptor the field being evaluated
   * @param rulesBuilder a mutable builder of the field's {@link FieldRules}; covered rules are
   *     cleared in place
   * @param valueEvaluator the value evaluator the native evaluator will be appended to
   * @return a native {@link Evaluator}, or null if no native evaluator applies (CEL handles
   *     everything)
   */
  public static @Nullable Evaluator tryBuild(
      FieldDescriptor fieldDescriptor,
      FieldRules.Builder rulesBuilder,
      ValueEvaluator valueEvaluator) {
    boolean hasNestedRule = valueEvaluator.hasNestedRule();
    if (fieldDescriptor.isMapField() && !hasNestedRule) {
      return tryBuildMapRules(rulesBuilder, valueEvaluator);
    }
    if (fieldDescriptor.isRepeated() && !hasNestedRule) {
      return tryBuildRepeatedRules(rulesBuilder, valueEvaluator);
    }
    if (!fieldDescriptor.isMapField() && !fieldDescriptor.isRepeated()) {
      return tryBuildScalarRules(fieldDescriptor, rulesBuilder, valueEvaluator);
    }
    return null;
  }

  // Phase 1: dispatcher skeleton. Per-kind builders are introduced in subsequent phases.
  // Phase 2 wires bool, Phase 3 numeric, Phase 4 enum, Phase 5 bytes, Phase 6 string,
  // Phase 7 repeated/map.

  @SuppressWarnings("unused")
  private static @Nullable Evaluator tryBuildScalarRules(
      FieldDescriptor fieldDescriptor,
      FieldRules.Builder rulesBuilder,
      ValueEvaluator valueEvaluator) {
    return null;
  }

  @SuppressWarnings("unused")
  private static @Nullable Evaluator tryBuildRepeatedRules(
      FieldRules.Builder rulesBuilder, ValueEvaluator valueEvaluator) {
    return null;
  }

  @SuppressWarnings("unused")
  private static @Nullable Evaluator tryBuildMapRules(
      FieldRules.Builder rulesBuilder, ValueEvaluator valueEvaluator) {
    return null;
  }
}
