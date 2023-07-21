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

package build.buf.protovalidate.internal.celext;

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.List;
import org.projectnessie.cel.common.types.BoolT;
import org.projectnessie.cel.common.types.BytesT;
import org.projectnessie.cel.common.types.DoubleT;
import org.projectnessie.cel.common.types.DurationT;
import org.projectnessie.cel.common.types.Err;
import org.projectnessie.cel.common.types.IntT;
import org.projectnessie.cel.common.types.ListT;
import org.projectnessie.cel.common.types.MapT;
import org.projectnessie.cel.common.types.NullT;
import org.projectnessie.cel.common.types.StringT;
import org.projectnessie.cel.common.types.TimestampT;
import org.projectnessie.cel.common.types.UintT;
import org.projectnessie.cel.common.types.pb.Db;
import org.projectnessie.cel.common.types.pb.DefaultTypeAdapter;
import org.projectnessie.cel.common.types.ref.Type;
import org.projectnessie.cel.common.types.ref.Val;

/** String formatter for CEL evaluation. */
final class Format {
  private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
  private static final char[] LOWER_HEX_ARRAY = "0123456789abcdef".toCharArray();

  /**
   * Format the string with a {@link ListT}.
   *
   * @param fmtString the string to format.
   * @param list the arguments.
   * @return the formatted string in {@link Val} form.
   */
  static Val format(String fmtString, ListT list) {
    // StringBuilder to accumulate the formatted string
    StringBuilder builder = new StringBuilder();
    int index = 0;
    int argIndex = 0;
    while (index < fmtString.length()) {
      char c = fmtString.charAt(index++);
      if (c != '%') {
        // Append non-format characters directly
        builder.append(c);
        // Add the entire character if it's not a UTF-8 character.
        if ((c & 0x80) != 0) {
          // Add the rest of the UTF-8 character.
          while (index < fmtString.length() && (fmtString.charAt(index) & 0xc0) == 0x80) {
            builder.append(fmtString.charAt(index++));
          }
        }
        continue;
      }
      if (index >= fmtString.length()) {
        return Err.newErr("format: expected format specifier");
      }
      if (fmtString.charAt(index) == '%') {
        // Escaped '%', append '%' and move to the next character
        builder.append('%');
        index++;
        continue;
      }
      if (argIndex >= list.size().intValue()) {
        return Err.newErr("format: not enough arguments");
      }
      Val arg = list.get(IntT.intOf(argIndex++));
      c = fmtString.charAt(index++);
      int precision = 6;
      if (c == '.') {
        // parse the precision
        precision = 0;
        while (index < fmtString.length()
            && '0' <= fmtString.charAt(index)
            && fmtString.charAt(index) <= '9') {
          precision = precision * 10 + (fmtString.charAt(index++) - '0');
        }
        if (index >= fmtString.length()) {
          return Err.newErr("format: expected format specifier");
        }
        c = fmtString.charAt(index++);
      }

      Val status;
      switch (c) {
        case 'd':
          status = formatDecimal(builder, arg);
          break;
        case 'x':
          status = formatHex(builder, arg, LOWER_HEX_ARRAY);
          break;
        case 'X':
          status = formatHex(builder, arg, HEX_ARRAY);
          break;
        case 's':
          status = formatString(builder, arg);
          break;
        case 'e':
        case 'f':
        case 'b':
        case 'o':
        default:
          return Err.newErr("format: unparsable format specifier %s", c);
      }
      if (status.type() == Err.ErrType) {
        return status;
      }
    }
    return StringT.stringOf(builder.toString());
  }

  /**
   * Converts a byte array to a hexadecimal string representation.
   *
   * @param bytes the byte array to convert.
   * @param digits the array of hexadecimal digits.
   * @return the hexadecimal string representation.
   */
  private static String bytesToHex(byte[] bytes, char[] digits) {
    char[] hexChars = new char[bytes.length * 2];
    for (int j = 0; j < bytes.length; j++) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = digits[v >>> 4];
      hexChars[j * 2 + 1] = digits[v & 0x0F];
    }
    return new String(hexChars);
  }

  /**
   * Formats a string value.
   *
   * @param builder the StringBuilder to append the formatted string to.
   * @param val the value to format.
   * @return the formatted string value.
   */
  private static Val formatString(StringBuilder builder, Val val) {
    if (val.type() == StringT.StringType) {
      builder.append(val.value());
      return NullT.NullValue;
    } else if (val.type() == BytesT.BytesType) {
      builder.append(val.value());
      return NullT.NullValue;
    } else {
      return formatStringSafe(builder, val, false);
    }
  }

  /**
   * Formats a string value safely for other value types.
   *
   * @param builder the StringBuilder to append the formatted string to.
   * @param val the value to format.
   * @param listType indicates if the value type is a list.
   * @return the formatted string value.
   */
  private static Val formatStringSafe(StringBuilder builder, Val val, boolean listType) {
    Type type = val.type();
    if (type == BoolT.BoolType) {
      builder.append(val.booleanValue());
    } else if (type == IntT.IntType || type == UintT.UintType) {
      formatInteger(builder, Long.valueOf(val.intValue()).intValue());
    } else if (type == DoubleT.DoubleType) {
      DecimalFormat format = new DecimalFormat("0.#");
      builder.append(format.format(val.value()));
    } else if (type == StringT.StringType) {
      builder.append("\"").append(val.value().toString()).append("\"");
    } else if (type == BytesT.BytesType) {
      formatBytes(builder, val);
    } else if (type == DurationT.DurationType) {
      formatDuration(builder, val, listType);
    } else if (type == TimestampT.TimestampType) {
      formatTimestamp(builder, val);
    } else if (type == ListT.ListType) {
      formatList(builder, val);
    } else if (type == MapT.MapType) {
      throw new RuntimeException("unimplemented stringSafe map type");
    } else if (type == NullT.NullType) {
      throw new RuntimeException("unimplemented stringSafe null type");
    }
    return val;
  }

  /**
   * Formats a list value.
   *
   * @param builder the StringBuilder to append the formatted list value to.
   * @param val the value to format.
   */
  private static void formatList(StringBuilder builder, Val val) {
    builder.append('[');
    List list = val.convertToNative(List.class);
    for (int i = 0; i < list.size(); i++) {
      Object obj = list.get(i);
      formatStringSafe(builder, DefaultTypeAdapter.nativeToValue(Db.newDb(), null, obj), true);
      if (i != list.size() - 1) {
        builder.append(", ");
      }
    }
    builder.append(']');
  }

  /**
   * Formats a timestamp value.
   *
   * @param builder the StringBuilder to append the formatted timestamp value to.
   * @param val the value to format.
   */
  private static void formatTimestamp(StringBuilder builder, Val val) {
    builder.append("timestamp(");
    Timestamp timestamp = val.convertToNative(Timestamp.class);
    builder.append(timestamp.toString());
    builder.append(")");
  }

  /**
   * Formats a duration value.
   *
   * @param builder the StringBuilder to append the formatted duration value to.
   * @param val the value to format.
   * @param listType indicates if the value type is a list.
   */
  private static void formatDuration(StringBuilder builder, Val val, boolean listType) {
    if (listType) {
      builder.append("duration(\"");
    }
    Duration duration = val.convertToNative(Duration.class);

    double totalSeconds = duration.getSeconds() + (duration.getNanos() / 1_000_000_000.0);

    DecimalFormat format = new DecimalFormat("0.#########");
    builder.append(format.format(totalSeconds));
    builder.append("s");
    if (listType) {
      builder.append("\")");
    }
  }

  /**
   * Formats a byte array value.
   *
   * @param builder the StringBuilder to append the formatted byte array value to.
   * @param val the value to format.
   */
  private static void formatBytes(StringBuilder builder, Val val) {
    builder
        .append("\"")
        .append(new String((byte[]) val.value(), StandardCharsets.UTF_8))
        .append("\"");
  }

  /**
   * Formats an integer value.
   *
   * @param builder the StringBuilder to append the formatted integer value to.
   * @param value the value to format.
   */
  private static void formatInteger(StringBuilder builder, int value) {
    if (value < 0) {
      builder.append("-");
      value = -value;
    }
    builder.append(value);
  }

  /**
   * Formats a hexadecimal value.
   *
   * @param builder the StringBuilder to append the formatted hexadecimal value to.
   * @param val the value to format.
   * @param digits the array of hexadecimal digits.
   * @return the formatted hexadecimal value.
   */
  private static Val formatHex(StringBuilder builder, Val val, char[] digits) {
    String hexString;
    if (val.type() == IntT.IntType || val.type() == UintT.UintType) {
      hexString = Long.toHexString(val.intValue());
    } else if (val.type() == BytesT.BytesType) {
      byte[] bytes = (byte[]) val.value();
      hexString = bytesToHex(bytes, digits);
    } else if (val.type() == StringT.StringType) {
      hexString = val.value().toString();
    } else {
      throw new RuntimeException("formatHex: expected int or string");
    }
    builder.append(hexString);
    return NullT.NullType;
  }

  /**
   * Formats a decimal value.
   *
   * @param builder the StringBuilder to append the formatted decimal value to.
   * @param arg the value to format.
   * @return the formatted decimal value.
   */
  private static Val formatDecimal(StringBuilder builder, Val arg) {
    builder.append(arg.value());
    return NullT.NullValue;
  }
}
