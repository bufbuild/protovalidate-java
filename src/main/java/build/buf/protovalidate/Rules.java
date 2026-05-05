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
      return MapRulesEvaluator.tryBuild(RuleBase.of(valueEvaluator), rulesBuilder);
    }
    if (fieldDescriptor.isRepeated() && !hasNestedRule) {
      return RepeatedRulesEvaluator.tryBuild(RuleBase.of(valueEvaluator), rulesBuilder);
    }
    if (!fieldDescriptor.isMapField() && !fieldDescriptor.isRepeated()) {
      Evaluator scalar = tryBuildScalarRules(fieldDescriptor, rulesBuilder, valueEvaluator);
      if (scalar == null) {
        return null;
      }
      // When processWrapperRules recurses with the inner "value" field, the ValueEvaluator's
      // descriptor is still the OUTER wrapper field. Detect that and wrap the scalar evaluator
      // so it unwraps the wrapper Message at evaluation time before delegating.
      FieldDescriptor outerDescriptor = valueEvaluator.getDescriptor();
      if (outerDescriptor != null
          && outerDescriptor.getJavaType() == FieldDescriptor.JavaType.MESSAGE) {
        return new WrappedValueEvaluator(fieldDescriptor, scalar);
      }
      return scalar;
    }
    return null;
  }

  private static @Nullable Evaluator tryBuildScalarRules(
      FieldDescriptor fieldDescriptor,
      FieldRules.Builder rulesBuilder,
      ValueEvaluator valueEvaluator) {
    RuleBase base = RuleBase.of(valueEvaluator);
    switch (fieldDescriptor.getJavaType()) {
      case BOOLEAN:
        return BoolRulesEvaluator.tryBuild(base, rulesBuilder);
      case ENUM:
        return EnumRulesEvaluator.tryBuild(base, rulesBuilder);
      case BYTE_STRING:
        return BytesRulesEvaluator.tryBuild(base, rulesBuilder);
      case STRING:
        return StringRulesEvaluator.tryBuild(base, rulesBuilder);
      case INT:
      case LONG:
      case FLOAT:
      case DOUBLE:
        NumericTypeConfig<?> config = numericConfigFor(fieldDescriptor);
        if (config == null) {
          return null;
        }
        return numericTryBuild(base, rulesBuilder, config);
      default:
        return null;
    }
  }

  private static @Nullable NumericTypeConfig<?> numericConfigFor(FieldDescriptor fd) {
    switch (fd.getType()) {
      case INT32:
        return NumericTypeConfig.INT32;
      case SINT32:
        return NumericTypeConfig.SINT32;
      case SFIXED32:
        return NumericTypeConfig.SFIXED32;
      case UINT32:
        return NumericTypeConfig.UINT32;
      case FIXED32:
        return NumericTypeConfig.FIXED32;
      case INT64:
        return NumericTypeConfig.INT64;
      case SINT64:
        return NumericTypeConfig.SINT64;
      case SFIXED64:
        return NumericTypeConfig.SFIXED64;
      case UINT64:
        return NumericTypeConfig.UINT64;
      case FIXED64:
        return NumericTypeConfig.FIXED64;
      case FLOAT:
        return NumericTypeConfig.FLOAT;
      case DOUBLE:
        return NumericTypeConfig.DOUBLE;
      default:
        return null;
    }
  }

  /**
   * Helper that captures the {@code <T>} on {@link NumericTypeConfig} so {@link
   * NumericRulesEvaluator#tryBuild} compiles cleanly. The unchecked cast is sound because the
   * config's generic parameter is the same as the evaluator's.
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  private static @Nullable Evaluator numericTryBuild(
      RuleBase base, FieldRules.Builder rulesBuilder, NumericTypeConfig<?> config) {
    return NumericRulesEvaluator.tryBuild(base, rulesBuilder, (NumericTypeConfig) config);
  }
}
