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

package build.buf.protovalidate;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import org.projectnessie.cel.common.types.Err.ErrException;
import org.projectnessie.cel.common.types.IntT;
import org.projectnessie.cel.common.types.IteratorT;
import org.projectnessie.cel.common.types.ListT;
import org.projectnessie.cel.common.types.MapT;
import org.projectnessie.cel.common.types.ref.TypeEnum;
import org.projectnessie.cel.common.types.ref.Val;

/** String formatter for CEL evaluation. */
final class Format {
  /**
   * Format the string with a {@link ListT}.
   *
   * @param fmtString the string to format.
   * @param list the arguments.
   * @return the formatted string.
   * @throws ErrException If an error occurs formatting the string.
   */
  static String format(String fmtString, ListT list) {
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
        throw new ErrException("format: expected format specifier");
      }
      if (fmtString.charAt(index) == '%') {
        // Escaped '%', append '%' and move to the next character
        builder.append('%');
        index++;
        continue;
      }
      if (argIndex >= list.size().intValue()) {
        throw new ErrException("index " + argIndex + " out of range");
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
          throw new ErrException("format: expected format specifier");
        }
        c = fmtString.charAt(index++);
      }

      switch (c) {
        case 'd':
          builder.append(formatDecimal(arg));
          break;
        case 'x':
          builder.append(formatHex(arg));
          break;
        case 'X':
          // We can use a root locale, because the only characters are hex (A-F).
          builder.append(formatHex(arg).toUpperCase(Locale.ROOT));
          break;
        case 's':
          builder.append(formatString(arg));
          break;
        case 'e':
          builder.append(formatExponential(arg, precision));
          break;
        case 'f':
          builder.append(formatFloat(arg, precision));
          break;
        case 'b':
          builder.append(formatBinary(arg));
          break;
        case 'o':
          builder.append(formatOctal(arg));
          break;
        default:
          throw new ErrException(
              "could not parse formatting clause: unrecognized formatting clause \"" + c + "\"");
      }
    }
    return builder.toString();
  }

  /**
   * Formats a string value.
   *
   * @param val the value to format.
   */
  private static String formatString(Val val) {
    TypeEnum type = val.type().typeEnum();
    switch (type) {
      case Bool:
        return Boolean.toString(val.booleanValue());
      case String:
        return val.value().toString();
      case Int:
      case Uint:
        Optional<String> str = validateNumber(val);
        if (str.isPresent()) {
          return str.get();
        }
        return val.value().toString();
      case Bytes:
        return new String((byte[]) val.value(), StandardCharsets.UTF_8);
      case Double:
        Optional<String> result = validateNumber(val);
        if (result.isPresent()) {
          return result.get();
        }
        return formatDecimal(val);
      case Duration:
        return formatDuration(val);
      case Timestamp:
        return formatTimestamp(val);
      case List:
        return formatList((ListT) val);
      case Map:
        return formatMap((MapT) val);
      case Null:
        return "null";
      default:
        throw new ErrException(
            "error during formatting: string clause can only be used on strings, bools, bytes, ints, doubles, maps, lists, types, durations, and timestamps, was given "
                + val.type());
    }
  }

  /**
   * Formats a list value.
   *
   * @param val the value to format.
   */
  private static String formatList(ListT val) {
    StringBuilder builder = new StringBuilder();
    builder.append('[');

    IteratorT iter = val.iterator();
    int index = 0;
    while (iter.hasNext().booleanValue()) {
      Val v = iter.next();
      builder.append(formatString(v));
      if (index != val.size().intValue() - 1) {
        builder.append(", ");
      }
      index++;
    }
    builder.append(']');
    return builder.toString();
  }

  private static String formatMap(MapT val) {
    StringBuilder builder = new StringBuilder();
    builder.append('{');

    IteratorT iter = val.iterator();
    int index = 0;
    while (iter.hasNext().booleanValue()) {
      Val key = iter.next();
      String mapKey = formatString(key);
      String mapVal = formatString(val.find(key));
      builder.append(mapKey).append(": ").append(mapVal);
      if (index != val.size().intValue() - 1) {
        builder.append(", ");
      }
      index++;
    }
    builder.append('}');
    return builder.toString();
  }

  /**
   * Formats a timestamp value.
   *
   * @param val the value to format.
   */
  private static String formatTimestamp(Val val) {
    Timestamp timestamp = val.convertToNative(Timestamp.class);
    Instant instant = Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    return ISO_INSTANT.format(instant);
  }

  /**
   * Formats a duration value.
   *
   * @param val the value to format.
   */
  private static String formatDuration(Val val) {
    StringBuilder builder = new StringBuilder();
    Duration duration = val.convertToNative(Duration.class);

    double totalSeconds = duration.getSeconds() + (duration.getNanos() / 1_000_000_000.0);

    DecimalFormat formatter = new DecimalFormat("0.#########");
    builder.append(formatter.format(totalSeconds));
    builder.append("s");

    return builder.toString();
  }

  /**
   * Formats a hexadecimal value.
   *
   * @param val the value to format.
   */
  private static String formatHex(Val val) {
    TypeEnum type = val.type().typeEnum();
    if (type == TypeEnum.Int || type == TypeEnum.Uint) {
      return Long.toHexString(val.intValue());
    } else if (type == TypeEnum.Bytes) {
      StringBuilder hexString = new StringBuilder();
      for (byte b : (byte[]) val.value()) {
        hexString.append(String.format("%02x", b));
      }
      return hexString.toString();
    } else if (type == TypeEnum.String) {
      String arg = val.value().toString();
      return String.format("%x", new BigInteger(1, arg.getBytes(StandardCharsets.UTF_8)));
    } else {
      throw new ErrException(
          "error during formatting: only integers, byte buffers, and strings can be formatted as hex, was given "
              + typeToString(val));
    }
  }

  /**
   * Formats a decimal value.
   *
   * @param val the value to format.
   */
  private static String formatDecimal(Val val) {
    TypeEnum type = val.type().typeEnum();
    if (type == TypeEnum.Int || type == TypeEnum.Uint || type == TypeEnum.Double) {
      Optional<String> str = validateNumber(val);
      if (str.isPresent()) {
        return str.get();
      }
      DecimalFormat formatter = new DecimalFormat("0.#########");
      return formatter.format(val.value());
    } else {
      throw new ErrException(
          "error during formatting: decimal clause can only be used on integers, was given "
              + typeToString(val));
    }
  }

  private static String formatOctal(Val val) {
    TypeEnum type = val.type().typeEnum();
    if (type == TypeEnum.Int || type == TypeEnum.Uint) {
      return Long.toOctalString(Long.valueOf(val.intValue()));
    } else {
      throw new ErrException(
          "error during formatting: octal clause can only be used on integers, was given "
              + typeToString(val));
    }
  }

  private static String formatBinary(Val val) {
    TypeEnum type = val.type().typeEnum();
    if (type == TypeEnum.Int || type == TypeEnum.Uint) {
      return Long.toBinaryString(Long.valueOf(val.intValue()));
    } else if (type == TypeEnum.Bool) {
      return val.booleanValue() ? "1" : "0";
    } else {
      throw new ErrException(
          "error during formatting: only integers and bools can be formatted as binary, was given "
              + typeToString(val));
    }
  }

  private static String formatExponential(Val val, int precision) {
    TypeEnum type = val.type().typeEnum();
    if (type == TypeEnum.Double) {
      Optional<String> str = validateNumber(val);
      if (str.isPresent()) {
        return str.get();
      }
      String pattern = "%." + precision + "e";
      return String.format(pattern.toString(), val.doubleValue());
    } else {
      throw new ErrException(
          "error during formatting: scientific clause can only be used on doubles, was given "
              + typeToString(val));
    }
  }

  private static String formatFloat(Val val, int precision) {
    TypeEnum type = val.type().typeEnum();
    if (type == TypeEnum.Double) {
      Optional<String> str = validateNumber(val);
      if (str.isPresent()) {
        return str.get();
      }
      StringBuilder pattern = new StringBuilder("0.");
      if (precision > 0) {
        for (int i = 0; i < precision; i++) {
          pattern.append("0");
        }
      } else {
        pattern.append("########");
      }
      DecimalFormat formatter = new DecimalFormat(pattern.toString());
      return formatter.format(val.value());
    } else {
      throw new ErrException(
          "error during formatting: fixed-point clause can only be used on doubles, was given "
              + typeToString(val));
    }
  }

  private static Optional<String> validateNumber(Val val) {
    if (val.doubleValue() == Double.POSITIVE_INFINITY) {
      return Optional.of("Infinity");
    } else if (val.doubleValue() == Double.NEGATIVE_INFINITY) {
      return Optional.of("-Infinity");
    }
    return Optional.empty();
  }

  private static String typeToString(Val val) {
    TypeEnum type = val.type().typeEnum();
    return type.getName();
  }
}
