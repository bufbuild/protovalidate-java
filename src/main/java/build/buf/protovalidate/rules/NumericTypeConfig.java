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

import build.buf.validate.DoubleRules;
import build.buf.validate.FieldRules;
import build.buf.validate.Fixed32Rules;
import build.buf.validate.Fixed64Rules;
import build.buf.validate.FloatRules;
import build.buf.validate.Int32Rules;
import build.buf.validate.Int64Rules;
import build.buf.validate.SFixed32Rules;
import build.buf.validate.SFixed64Rules;
import build.buf.validate.SInt32Rules;
import build.buf.validate.SInt64Rules;
import build.buf.validate.UInt32Rules;
import build.buf.validate.UInt64Rules;
import com.google.protobuf.Descriptors.FieldDescriptor;
import java.util.Comparator;
import java.util.function.Function;

/**
 * Per-kind type config for native numeric rule evaluation. Bundles everything that varies between
 * proto numeric kinds (int32, uint32, float, etc.): descriptor lookups, the boxed Java type, the
 * comparator (signed vs unsigned), the value-to-string formatter, and a flag for whether NaN fails
 * range checks.
 *
 * <p>One static instance per kind, shared across every {@link NumericRulesEvaluator} for that kind.
 */
final class NumericTypeConfig<T extends Number & Comparable<T>> {
  /** Proto rule prefix used in rule ids ({@code "int32"}, {@code "uint64"}, etc.). */
  final String typeName;

  /** Pre-built rule sites and field descriptors for this kind. */
  final NumericDescriptors descriptors;

  /** Boxed Java class for values of this kind. Used to extract values from {@code Value}. */
  final Class<T> valueClass;

  /**
   * Comparator over values of this kind. For signed/float kinds this is the natural order; for
   * unsigned kinds it is {@code Integer.compareUnsigned}/{@code Long.compareUnsigned} since Java
   * stores unsigned protobuf values in signed primitives.
   */
  final Comparator<T> comparator;

  /**
   * Renders a value to the string used in violation messages. {@code String::valueOf} for signed
   * and float kinds; {@code Integer::toUnsignedString}/{@code Long::toUnsignedString} for unsigned.
   * Critical for unsigned kinds — {@code String.valueOf(int)} would print a negative integer for
   * values whose unsigned representation exceeds {@code Integer.MAX_VALUE}.
   */
  final Function<T, String> formatter;

  /**
   * True for {@code float}/{@code double}: NaN fails range checks (matches CEL semantics). False
   * for integer kinds — they have no NaN.
   */
  final boolean nanFailsRange;

  private NumericTypeConfig(
      String typeName,
      NumericDescriptors descriptors,
      Class<T> valueClass,
      Comparator<T> comparator,
      Function<T, String> formatter,
      boolean nanFailsRange) {
    this.typeName = typeName;
    this.descriptors = descriptors;
    this.valueClass = valueClass;
    this.comparator = comparator;
    this.formatter = formatter;
    this.nanFailsRange = nanFailsRange;
  }

  // --- Static configs, one per proto numeric kind ---

  private static FieldDescriptor frField(int number) {
    return FieldRules.getDescriptor().findFieldByNumber(number);
  }

  static final NumericTypeConfig<Integer> INT32 =
      new NumericTypeConfig<>(
          "int32",
          NumericDescriptors.build(
              frField(FieldRules.INT32_FIELD_NUMBER), Int32Rules.getDescriptor(), "int32", false),
          Integer.class,
          Integer::compare,
          String::valueOf,
          false);

  static final NumericTypeConfig<Integer> SINT32 =
      new NumericTypeConfig<>(
          "sint32",
          NumericDescriptors.build(
              frField(FieldRules.SINT32_FIELD_NUMBER),
              SInt32Rules.getDescriptor(),
              "sint32",
              false),
          Integer.class,
          Integer::compare,
          String::valueOf,
          false);

  static final NumericTypeConfig<Integer> SFIXED32 =
      new NumericTypeConfig<>(
          "sfixed32",
          NumericDescriptors.build(
              frField(FieldRules.SFIXED32_FIELD_NUMBER),
              SFixed32Rules.getDescriptor(),
              "sfixed32",
              false),
          Integer.class,
          Integer::compare,
          String::valueOf,
          false);

  static final NumericTypeConfig<Integer> UINT32 =
      new NumericTypeConfig<>(
          "uint32",
          NumericDescriptors.build(
              frField(FieldRules.UINT32_FIELD_NUMBER),
              UInt32Rules.getDescriptor(),
              "uint32",
              false),
          Integer.class,
          Integer::compareUnsigned,
          Integer::toUnsignedString,
          false);

  static final NumericTypeConfig<Integer> FIXED32 =
      new NumericTypeConfig<>(
          "fixed32",
          NumericDescriptors.build(
              frField(FieldRules.FIXED32_FIELD_NUMBER),
              Fixed32Rules.getDescriptor(),
              "fixed32",
              false),
          Integer.class,
          Integer::compareUnsigned,
          Integer::toUnsignedString,
          false);

  static final NumericTypeConfig<Long> INT64 =
      new NumericTypeConfig<>(
          "int64",
          NumericDescriptors.build(
              frField(FieldRules.INT64_FIELD_NUMBER), Int64Rules.getDescriptor(), "int64", false),
          Long.class,
          Long::compare,
          String::valueOf,
          false);

  static final NumericTypeConfig<Long> SINT64 =
      new NumericTypeConfig<>(
          "sint64",
          NumericDescriptors.build(
              frField(FieldRules.SINT64_FIELD_NUMBER),
              SInt64Rules.getDescriptor(),
              "sint64",
              false),
          Long.class,
          Long::compare,
          String::valueOf,
          false);

  static final NumericTypeConfig<Long> SFIXED64 =
      new NumericTypeConfig<>(
          "sfixed64",
          NumericDescriptors.build(
              frField(FieldRules.SFIXED64_FIELD_NUMBER),
              SFixed64Rules.getDescriptor(),
              "sfixed64",
              false),
          Long.class,
          Long::compare,
          String::valueOf,
          false);

  static final NumericTypeConfig<Long> UINT64 =
      new NumericTypeConfig<>(
          "uint64",
          NumericDescriptors.build(
              frField(FieldRules.UINT64_FIELD_NUMBER),
              UInt64Rules.getDescriptor(),
              "uint64",
              false),
          Long.class,
          Long::compareUnsigned,
          Long::toUnsignedString,
          false);

  static final NumericTypeConfig<Long> FIXED64 =
      new NumericTypeConfig<>(
          "fixed64",
          NumericDescriptors.build(
              frField(FieldRules.FIXED64_FIELD_NUMBER),
              Fixed64Rules.getDescriptor(),
              "fixed64",
              false),
          Long.class,
          Long::compareUnsigned,
          Long::toUnsignedString,
          false);

  static final NumericTypeConfig<Float> FLOAT =
      new NumericTypeConfig<>(
          "float",
          NumericDescriptors.build(
              frField(FieldRules.FLOAT_FIELD_NUMBER), FloatRules.getDescriptor(), "float", true),
          Float.class,
          Float::compare,
          NumericTypeConfig::floatFormatter,
          true);

  static final NumericTypeConfig<Double> DOUBLE =
      new NumericTypeConfig<>(
          "double",
          NumericDescriptors.build(
              frField(FieldRules.DOUBLE_FIELD_NUMBER), DoubleRules.getDescriptor(), "double", true),
          Double.class,
          Double::compare,
          NumericTypeConfig::floatFormatter,
          true);

  public static String floatFormatter(Object obj) {
    if (obj instanceof Float) {
      // if the float is a whole number, don't print the decimal
      Float f = (Float) obj;
      float f2 = f.intValue();
      if (f2 == f) {
        return String.valueOf(f.intValue());
      }
      return String.valueOf(f);
    }
    if (obj instanceof Double) {
      // if the float is a whole number, don't print the decimal
      Double d = (Double) obj;
      double d2 = d.intValue();
      if (d2 == d) {
        return String.valueOf(d.intValue());
      }
      return String.valueOf(d);
    }
    return String.valueOf(obj);
  }
}
