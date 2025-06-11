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

import com.google.common.primitives.UnsignedLong;
import com.google.protobuf.ByteString;
import com.google.protobuf.Duration;
import com.google.protobuf.NullValue;
import com.google.protobuf.Timestamp;
import dev.cel.common.types.TypeType;
import dev.cel.runtime.CelEvaluationException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** String formatter for CEL evaluation. */
final class Format {
  /**
   * Format the string with a {@link List}.
   *
   * @param fmtString the string to format.
   * @param list the arguments.
   * @return the formatted string.
   * @throws CelEvaluationException If an error occurs formatting the string.
   */
  static String format(String fmtString, List<?> list) throws CelEvaluationException {
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
        throw new CelEvaluationException("format: expected format specifier");
      }
      if (fmtString.charAt(index) == '%') {
        // Escaped '%', append '%' and move to the next character
        builder.append('%');
        index++;
        continue;
      }
      if (argIndex >= list.size()) {
        throw new CelEvaluationException("index " + argIndex + " out of range");
      }
      Object arg = list.get(argIndex++);
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
          throw new CelEvaluationException("format: expected format specifier");
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
          throw new CelEvaluationException(
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
  private static String formatString(Object val) throws CelEvaluationException {
    if (val instanceof String) {
      return (String) val;
    } else if (val instanceof TypeType) {
      return ((TypeType) val).containingTypeName();
    } else if (val instanceof Boolean) {
      return Boolean.toString((Boolean) val);
    } else if (val instanceof Long || val instanceof UnsignedLong) {
      Optional<String> str = validateNumber(val);
      if (str.isPresent()) {
        return str.get();
      }
      return val.toString();
    } else if (val instanceof ByteString) {
      String byteStr = ((ByteString) val).toStringUtf8();
      // Collapse any contiguous placeholders into one
      return byteStr.replaceAll("\\ufffd+", "\ufffd");
    } else if (val instanceof Double) {
      Optional<String> result = validateNumber(val);
      if (result.isPresent()) {
        return result.get();
      }
      return formatDecimal(val);
    } else if (val instanceof Duration) {
      return formatDuration((Duration) val);
    } else if (val instanceof Timestamp) {
      return formatTimestamp((Timestamp) val);
    } else if (val instanceof List) {
      return formatList((List<?>) val);
    } else if (val instanceof Map) {
      return formatMap((Map<?, ?>) val);
    } else if (val == null || val instanceof NullValue) {
      return "null";
    }
    throw new CelEvaluationException(
        "error during formatting: string clause can only be used on strings, bools, bytes, ints, doubles, maps, lists, types, durations, and timestamps, was given "
            + val.getClass());
  }

  /**
   * Formats a list value.
   *
   * @param val the value to format.
   */
  private static String formatList(List<?> val) throws CelEvaluationException {
    StringBuilder builder = new StringBuilder();
    builder.append('[');

    Iterator<?> iter = val.iterator();
    while (iter.hasNext()) {
      Object v = iter.next();
      builder.append(formatString(v));
      if (iter.hasNext()) {
        builder.append(", ");
      }
    }
    builder.append(']');
    return builder.toString();
  }

  private static String formatMap(Map<?, ?> val) throws CelEvaluationException {
    StringBuilder builder = new StringBuilder();
    builder.append('{');

    SortedMap<String, String> sorted = new TreeMap<>();

    for (Entry<?, ?> entry : val.entrySet()) {
      sorted.put(formatString(entry.getKey()), formatString(entry.getValue()));
    }

    String result =
        sorted.entrySet().stream()
            .map(entry -> entry.getKey() + ": " + entry.getValue())
            .collect(Collectors.joining(", "));

    builder.append(result).append('}');

    return builder.toString();
  }

  /**
   * Formats a timestamp value.
   *
   * @param timestamp the value to format.
   */
  private static String formatTimestamp(Timestamp timestamp) {
    return ISO_INSTANT.format(Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos()));
  }

  /**
   * Formats a duration value.
   *
   * @param duration the value to format.
   */
  private static String formatDuration(Duration duration) {
    StringBuilder builder = new StringBuilder();

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
  private static String formatHex(Object val) throws CelEvaluationException {
    if (val instanceof Long) {
      return Long.toHexString((Long) val);
    } else if (val instanceof UnsignedLong) {
      return Long.toHexString(((UnsignedLong) val).longValue());
    } else if (val instanceof ByteString) {
      StringBuilder hexString = new StringBuilder();
      for (byte b : (ByteString) val) {
        hexString.append(String.format("%02x", b));
      }
      return hexString.toString();
    } else if (val instanceof String) {
      String arg = (String) val;
      return String.format("%x", new BigInteger(1, arg.getBytes(StandardCharsets.UTF_8)));
    } else {
      throw new CelEvaluationException(
          "error during formatting: only integers, byte buffers, and strings can be formatted as hex, was given "
              + val.getClass());
    }
  }

  /**
   * Formats a decimal value.
   *
   * @param val the value to format.
   */
  private static String formatDecimal(Object val) throws CelEvaluationException {
    if (val instanceof Long || val instanceof UnsignedLong || val instanceof Double) {
      Optional<String> str = validateNumber(val);
      if (str.isPresent()) {
        return str.get();
      }
      DecimalFormat formatter = new DecimalFormat("0.#########");
      return formatter.format(val);
    } else {
      throw new CelEvaluationException(
          "error during formatting: decimal clause can only be used on integers, was given "
              + val.getClass());
    }
  }

  private static String formatOctal(Object val) throws CelEvaluationException {
    if (val instanceof Long) {
      return Long.toOctalString((Long) val);
    } else if (val instanceof UnsignedLong) {
      return Long.toOctalString(((UnsignedLong) val).longValue());
    } else {
      throw new CelEvaluationException(
          "error during formatting: octal clause can only be used on integers, was given "
              + val.getClass());
    }
  }

  private static String formatBinary(Object val) throws CelEvaluationException {
    if (val instanceof Long) {
      return Long.toBinaryString((Long) val);
    } else if (val instanceof UnsignedLong) {
      return Long.toBinaryString(((UnsignedLong) val).longValue());
    } else if (val instanceof Boolean) {
      return Boolean.TRUE.equals(val) ? "1" : "0";
    } else {
      throw new CelEvaluationException(
          "error during formatting: only integers and bools can be formatted as binary, was given "
              + val.getClass());
    }
  }

  private static String formatExponential(Object val, int precision) throws CelEvaluationException {
    if (val instanceof Double) {
      Optional<String> str = validateNumber(val);
      if (str.isPresent()) {
        return str.get();
      }
      String pattern = "%." + precision + "e";
      return String.format(pattern, val);
    } else {
      throw new CelEvaluationException(
          "error during formatting: scientific clause can only be used on doubles, was given "
              + val.getClass());
    }
  }

  private static String formatFloat(Object val, int precision) throws CelEvaluationException {
    if (val instanceof Double) {
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
      return formatter.format(val);
    } else {
      throw new CelEvaluationException(
          "error during formatting: fixed-point clause can only be used on doubles, was given "
              + val.getClass());
    }
  }

  private static Optional<String> validateNumber(Object val) {
    if (val instanceof Double) {
      if ((Double) val == Double.POSITIVE_INFINITY) {
        return Optional.of("Infinity");
      } else if ((Double) val == Double.NEGATIVE_INFINITY) {
        return Optional.of("-Infinity");
      }
    }
    return Optional.empty();
  }
}
