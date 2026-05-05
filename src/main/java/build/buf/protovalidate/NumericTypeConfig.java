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

  /**
   * The {@link FieldRules} field descriptor for this numeric kind (e.g. the {@code int32} field on
   * {@code FieldRules}). Used by the dispatcher to read and clear the typed rules sub-message.
   */
  final FieldDescriptor rulesField;

  private NumericTypeConfig(
      String typeName,
      NumericDescriptors descriptors,
      Class<T> valueClass,
      Comparator<T> comparator,
      Function<T, String> formatter,
      boolean nanFailsRange,
      FieldDescriptor rulesField) {
    this.typeName = typeName;
    this.descriptors = descriptors;
    this.valueClass = valueClass;
    this.comparator = comparator;
    this.formatter = formatter;
    this.nanFailsRange = nanFailsRange;
    this.rulesField = rulesField;
  }

  // --- Static configs, one per proto numeric kind ---
  //
  // Class-init invariant: every static config below transitively calls FieldRules.getDescriptor()
  // and the per-kind *Rules.getDescriptor() through frField() and NumericDescriptors.build(). For
  // class-loading to succeed, FieldRules and the per-kind rules messages must be loadable when
  // NumericTypeConfig is. They are — FieldRules is touched by every entry into EvaluatorBuilder,
  // and the per-kind rules messages live in the same generated bundle. If a future change moves
  // NumericTypeConfig's initialization earlier (e.g. via a static reference from a class loaded
  // before FieldRules), expect NoClassDefFoundError on this class.

  private static FieldDescriptor frField(int number) {
    return FieldRules.getDescriptor().findFieldByNumber(number);
  }

  /** Builds a NumericTypeConfig and exposes the FieldRules-level field descriptor on it. */
  private static <T extends Number & Comparable<T>> NumericTypeConfig<T> create(
      String typeName,
      int fieldRulesFieldNumber,
      com.google.protobuf.Descriptors.Descriptor rulesDescriptor,
      Class<T> valueClass,
      Comparator<T> comparator,
      Function<T, String> formatter,
      boolean nanFailsRange) {
    FieldDescriptor rulesField = frField(fieldRulesFieldNumber);
    return new NumericTypeConfig<>(
        typeName,
        NumericDescriptors.build(rulesField, rulesDescriptor, typeName, nanFailsRange),
        valueClass,
        comparator,
        formatter,
        nanFailsRange,
        rulesField);
  }

  static final NumericTypeConfig<Integer> INT32 =
      create(
          "int32",
          FieldRules.INT32_FIELD_NUMBER,
          Int32Rules.getDescriptor(),
          Integer.class,
          Integer::compare,
          String::valueOf,
          false);

  static final NumericTypeConfig<Integer> SINT32 =
      create(
          "sint32",
          FieldRules.SINT32_FIELD_NUMBER,
          SInt32Rules.getDescriptor(),
          Integer.class,
          Integer::compare,
          String::valueOf,
          false);

  static final NumericTypeConfig<Integer> SFIXED32 =
      create(
          "sfixed32",
          FieldRules.SFIXED32_FIELD_NUMBER,
          SFixed32Rules.getDescriptor(),
          Integer.class,
          Integer::compare,
          String::valueOf,
          false);

  static final NumericTypeConfig<Integer> UINT32 =
      create(
          "uint32",
          FieldRules.UINT32_FIELD_NUMBER,
          UInt32Rules.getDescriptor(),
          Integer.class,
          Integer::compareUnsigned,
          Integer::toUnsignedString,
          false);

  static final NumericTypeConfig<Integer> FIXED32 =
      create(
          "fixed32",
          FieldRules.FIXED32_FIELD_NUMBER,
          Fixed32Rules.getDescriptor(),
          Integer.class,
          Integer::compareUnsigned,
          Integer::toUnsignedString,
          false);

  static final NumericTypeConfig<Long> INT64 =
      create(
          "int64",
          FieldRules.INT64_FIELD_NUMBER,
          Int64Rules.getDescriptor(),
          Long.class,
          Long::compare,
          String::valueOf,
          false);

  static final NumericTypeConfig<Long> SINT64 =
      create(
          "sint64",
          FieldRules.SINT64_FIELD_NUMBER,
          SInt64Rules.getDescriptor(),
          Long.class,
          Long::compare,
          String::valueOf,
          false);

  static final NumericTypeConfig<Long> SFIXED64 =
      create(
          "sfixed64",
          FieldRules.SFIXED64_FIELD_NUMBER,
          SFixed64Rules.getDescriptor(),
          Long.class,
          Long::compare,
          String::valueOf,
          false);

  static final NumericTypeConfig<Long> UINT64 =
      create(
          "uint64",
          FieldRules.UINT64_FIELD_NUMBER,
          UInt64Rules.getDescriptor(),
          Long.class,
          Long::compareUnsigned,
          Long::toUnsignedString,
          false);

  static final NumericTypeConfig<Long> FIXED64 =
      create(
          "fixed64",
          FieldRules.FIXED64_FIELD_NUMBER,
          Fixed64Rules.getDescriptor(),
          Long.class,
          Long::compareUnsigned,
          Long::toUnsignedString,
          false);

  static final NumericTypeConfig<Float> FLOAT =
      create(
          "float",
          FieldRules.FLOAT_FIELD_NUMBER,
          FloatRules.getDescriptor(),
          Float.class,
          NumericTypeConfig::floatCompare,
          NumericTypeConfig::floatFormatter,
          true);

  static final NumericTypeConfig<Double> DOUBLE =
      create(
          "double",
          FieldRules.DOUBLE_FIELD_NUMBER,
          DoubleRules.getDescriptor(),
          Double.class,
          NumericTypeConfig::doubleCompare,
          NumericTypeConfig::doubleFormatter,
          true);

  // Float and double comparators treat +0.0 and -0.0 as equal, matching IEEE-754. NaN keeps
  // Java's compareTo semantics (NaN.compareTo(NaN) == 0) so behavior matches the existing
  // protovalidate-java CEL path, which uses the same Object.equals semantics.

  private static int floatCompare(Float f1, Float f2) {
    if (f1 == 0.0f && f2 == 0.0f) {
      return 0;
    }
    return f1.compareTo(f2);
  }

  private static int doubleCompare(Double d1, Double d2) {
    if (d1 == 0.0 && d2 == 0.0) {
      return 0;
    }
    return d1.compareTo(d2);
  }

  private static final int FLOAT_NEG_ZERO_BITS = Float.floatToIntBits(-0.0f);
  private static final long DOUBLE_NEG_ZERO_BITS = Double.doubleToLongBits(-0.0);

  private static String floatFormatter(Float f) {
    if (Float.floatToIntBits(f) == FLOAT_NEG_ZERO_BITS) {
      return "-0";
    }
    // Whole-number short-circuit: print "5" rather than "5.0" to match Go's %g behavior.
    float asInt = f.intValue();
    if (asInt == f) {
      return String.valueOf(f.intValue());
    }
    return String.valueOf(f);
  }

  private static String doubleFormatter(Double d) {
    if (Double.doubleToLongBits(d) == DOUBLE_NEG_ZERO_BITS) {
      return "-0";
    }
    double asInt = d.intValue();
    if (asInt == d) {
      return String.valueOf(d.intValue());
    }
    return String.valueOf(d);
  }
}
