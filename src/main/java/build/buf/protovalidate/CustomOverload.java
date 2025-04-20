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

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.projectnessie.cel.common.types.BoolT;
import org.projectnessie.cel.common.types.Err;
import org.projectnessie.cel.common.types.IntT;
import org.projectnessie.cel.common.types.ListT;
import org.projectnessie.cel.common.types.StringT;
import org.projectnessie.cel.common.types.Types;
import org.projectnessie.cel.common.types.ref.TypeEnum;
import org.projectnessie.cel.common.types.ref.Val;
import org.projectnessie.cel.common.types.traits.Lister;
import org.projectnessie.cel.interpreter.functions.Overload;

/** Defines custom function overloads (the implementation). */
final class CustomOverload {

  private static final String OVERLOAD_FORMAT = "format";
  private static final String OVERLOAD_UNIQUE = "unique";
  private static final String OVERLOAD_STARTS_WITH = "startsWith";
  private static final String OVERLOAD_ENDS_WITH = "endsWith";
  private static final String OVERLOAD_CONTAINS = "contains";
  private static final String OVERLOAD_IS_HOSTNAME = "isHostname";
  private static final String OVERLOAD_IS_EMAIL = "isEmail";
  private static final String OVERLOAD_IS_IP = "isIp";
  private static final String OVERLOAD_IS_IP_PREFIX = "isIpPrefix";
  private static final String OVERLOAD_IS_URI = "isUri";
  private static final String OVERLOAD_IS_URI_REF = "isUriRef";
  private static final String OVERLOAD_IS_NAN = "isNan";
  private static final String OVERLOAD_IS_INF = "isInf";
  private static final String OVERLOAD_IS_HOST_AND_PORT = "isHostAndPort";

  // See https://html.spec.whatwg.org/multipage/input.html#valid-e-mail-address
  private static final Pattern EMAIL_REGEX =
      Pattern.compile(
          "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$");

  /**
   * Create custom function overload list.
   *
   * @return an array of overloaded functions.
   */
  static Overload[] create() {
    return new Overload[] {
      celFormat(),
      celUnique(),
      celStartsWith(),
      celEndsWith(),
      celContains(),
      celIsHostname(),
      celIsEmail(),
      celIsIp(),
      celIsIpPrefix(),
      celIsUri(),
      celIsUriRef(),
      celIsNan(),
      celIsInf(),
      celIsHostAndPort(),
    };
  }

  /**
   * Creates a custom binary function overload for the "format" operation.
   *
   * @return The {@link Overload} instance for the "format" operation.
   */
  private static Overload celFormat() {
    return Overload.binary(
        OVERLOAD_FORMAT,
        (lhs, rhs) -> {
          if (lhs.type().typeEnum() != TypeEnum.String || rhs.type().typeEnum() != TypeEnum.List) {
            return Err.noSuchOverload(lhs, OVERLOAD_FORMAT, rhs);
          }
          ListT list = (ListT) rhs.convertToType(ListT.ListType);
          String formatString = (String) lhs.value();
          try {
            return StringT.stringOf(Format.format(formatString, list));
          } catch (Err.ErrException e) {
            return e.getErr();
          }
        });
  }

  /**
   * Creates a custom unary function overload for the "unique" operation.
   *
   * @return The {@link Overload} instance for the "unique" operation.
   */
  private static Overload celUnique() {
    return Overload.unary(
        OVERLOAD_UNIQUE,
        (val) -> {
          if (val.type().typeEnum() != TypeEnum.List) {
            return Err.noSuchOverload(val, OVERLOAD_UNIQUE, null);
          }
          return uniqueList((Lister) val);
        });
  }

  /**
   * Creates a custom binary function overload for the "startsWith" operation.
   *
   * @return The {@link Overload} instance for the "startsWith" operation.
   */
  private static Overload celStartsWith() {
    return Overload.binary(
        OVERLOAD_STARTS_WITH,
        (lhs, rhs) -> {
          TypeEnum lhsType = lhs.type().typeEnum();
          if (lhsType != rhs.type().typeEnum()) {
            return Err.noSuchOverload(lhs, OVERLOAD_STARTS_WITH, rhs);
          }
          if (lhsType == TypeEnum.String) {
            String receiver = lhs.value().toString();
            String param = rhs.value().toString();
            return Types.boolOf(receiver.startsWith(param));
          }
          if (lhsType == TypeEnum.Bytes) {
            byte[] receiver = (byte[]) lhs.value();
            byte[] param = (byte[]) rhs.value();
            if (receiver.length < param.length) {
              return BoolT.False;
            }
            for (int i = 0; i < param.length; i++) {
              if (param[i] != receiver[i]) {
                return BoolT.False;
              }
            }
            return BoolT.True;
          }
          return Err.noSuchOverload(lhs, OVERLOAD_STARTS_WITH, rhs);
        });
  }

  /**
   * Creates a custom binary function overload for the "endsWith" operation.
   *
   * @return The {@link Overload} instance for the "endsWith" operation.
   */
  private static Overload celEndsWith() {
    return Overload.binary(
        OVERLOAD_ENDS_WITH,
        (lhs, rhs) -> {
          TypeEnum lhsType = lhs.type().typeEnum();
          if (lhsType != rhs.type().typeEnum()) {
            return Err.noSuchOverload(lhs, OVERLOAD_ENDS_WITH, rhs);
          }
          if (lhsType == TypeEnum.String) {
            String receiver = (String) lhs.value();
            String param = (String) rhs.value();
            return Types.boolOf(receiver.endsWith(param));
          }
          if (lhsType == TypeEnum.Bytes) {
            byte[] receiver = (byte[]) lhs.value();
            byte[] param = (byte[]) rhs.value();
            if (receiver.length < param.length) {
              return BoolT.False;
            }
            for (int i = 0; i < param.length; i++) {
              if (param[param.length - i - 1] != receiver[receiver.length - i - 1]) {
                return BoolT.False;
              }
            }
            return BoolT.True;
          }
          return Err.noSuchOverload(lhs, OVERLOAD_ENDS_WITH, rhs);
        });
  }

  /**
   * Creates a custom binary function overload for the "contains" operation.
   *
   * @return The {@link Overload} instance for the "contains" operation.
   */
  private static Overload celContains() {
    return Overload.binary(
        OVERLOAD_CONTAINS,
        (lhs, rhs) -> {
          TypeEnum lhsType = lhs.type().typeEnum();
          if (lhsType != rhs.type().typeEnum()) {
            return Err.noSuchOverload(lhs, OVERLOAD_CONTAINS, rhs);
          }
          if (lhsType == TypeEnum.String) {
            String receiver = lhs.value().toString();
            String param = rhs.value().toString();
            return Types.boolOf(receiver.contains(param));
          }
          if (lhsType == TypeEnum.Bytes) {
            byte[] receiver = (byte[]) lhs.value();
            byte[] param = (byte[]) rhs.value();
            return Types.boolOf(bytesContains(receiver, param));
          }
          return Err.noSuchOverload(lhs, OVERLOAD_CONTAINS, rhs);
        });
  }

  static boolean bytesContains(byte[] arr, byte[] subArr) {
    if (subArr.length == 0) {
      return true;
    }
    if (subArr.length > arr.length) {
      return false;
    }
    for (int i = 0; i < arr.length - subArr.length + 1; i++) {
      boolean found = true;
      for (int j = 0; j < subArr.length; j++) {
        if (arr[i + j] != subArr[j]) {
          found = false;
          break;
        }
      }
      if (found) {
        return true;
      }
    }
    return false;
  }

  /**
   * Creates a custom binary function overload for the "isHostname" operation.
   *
   * @return The {@link Overload} instance for the "isHostname" operation.
   */
  private static Overload celIsHostname() {
    return Overload.unary(
        OVERLOAD_IS_HOSTNAME,
        value -> {
          if (value.type().typeEnum() != TypeEnum.String) {
            return Err.noSuchOverload(value, OVERLOAD_IS_HOSTNAME, null);
          }
          String host = (String) value.value();
          return Types.boolOf(isHostname(host));
        });
  }

  /**
   * Creates a custom unary function overload for the "isEmail" operation.
   *
   * @return The {@link Overload} instance for the "isEmail" operation.
   */
  private static Overload celIsEmail() {
    return Overload.unary(
        OVERLOAD_IS_EMAIL,
        value -> {
          if (value.type().typeEnum() != TypeEnum.String) {
            return Err.noSuchOverload(value, OVERLOAD_IS_EMAIL, null);
          }
          String addr = (String) value.value();
          return Types.boolOf(isEmail(addr));
        });
  }

  /**
   * Creates a custom function overload for the "isIp" operation.
   *
   * @return The {@link Overload} instance for the "isIp" operation.
   */
  private static Overload celIsIp() {
    return Overload.overload(
        OVERLOAD_IS_IP,
        null,
        value -> {
          if (value.type().typeEnum() != TypeEnum.String) {
            return Err.noSuchOverload(value, OVERLOAD_IS_IP, null);
          }
          String addr = (String) value.value();
          return Types.boolOf(isIp(addr, 0L));
        },
        (lhs, rhs) -> {
          if (lhs.type().typeEnum() != TypeEnum.String || rhs.type().typeEnum() != TypeEnum.Int) {
            return Err.noSuchOverload(lhs, OVERLOAD_IS_IP, rhs);
          }
          String address = (String) lhs.value();
          return Types.boolOf(isIp(address, rhs.intValue()));
        },
        null);
  }

  /**
   * Creates a custom function overload for the "isIpPrefix" operation.
   *
   * @return The {@link Overload} instance for the "isIpPrefix" operation.
   */
  private static Overload celIsIpPrefix() {
    return Overload.overload(
        OVERLOAD_IS_IP_PREFIX,
        null,
        value -> {
          if (value.type().typeEnum() != TypeEnum.String
              && value.type().typeEnum() != TypeEnum.Bool) {
            return Err.noSuchOverload(value, OVERLOAD_IS_IP_PREFIX, null);
          }
          String prefix = (String) value.value();
          return Types.boolOf(isIpPrefix(prefix, 0L, false));
        },
        (lhs, rhs) -> {
          if (lhs.type().typeEnum() != TypeEnum.String
              || (rhs.type().typeEnum() != TypeEnum.Int
                  && rhs.type().typeEnum() != TypeEnum.Bool)) {
            return Err.noSuchOverload(lhs, OVERLOAD_IS_IP_PREFIX, rhs);
          }
          String prefix = (String) lhs.value();
          if (rhs.type().typeEnum() == TypeEnum.Int) {
            return Types.boolOf(isIpPrefix(prefix, rhs.intValue(), false));
          }
          return Types.boolOf(isIpPrefix(prefix, 0L, rhs.booleanValue()));
        },
        (values) -> {
          if (values.length != 3
              || values[0].type().typeEnum() != TypeEnum.String
              || values[1].type().typeEnum() != TypeEnum.Int
              || values[2].type().typeEnum() != TypeEnum.Bool) {
            return Err.noSuchOverload(values[0], OVERLOAD_IS_IP_PREFIX, "", values);
          }
          String prefix = (String) values[0].value();
          return Types.boolOf(isIpPrefix(prefix, values[1].intValue(), values[2].booleanValue()));
        });
  }

  /**
   * Creates a custom unary function overload for the "isUri" operation.
   *
   * @return The {@link Overload} instance for the "isUri" operation.
   */
  private static Overload celIsUri() {
    return Overload.unary(
        OVERLOAD_IS_URI,
        value -> {
          if (value.type().typeEnum() != TypeEnum.String) {
            return Err.noSuchOverload(value, OVERLOAD_IS_URI, null);
          }
          String addr = (String) value.value();
          return Types.boolOf(isUri(addr));
        });
  }

  /**
   * Creates a custom unary function overload for the "isUriRef" operation.
   *
   * @return The {@link Overload} instance for the "isUriRef" operation.
   */
  private static Overload celIsUriRef() {
    return Overload.unary(
        OVERLOAD_IS_URI_REF,
        value -> {
          if (value.type().typeEnum() != TypeEnum.String) {
            return Err.noSuchOverload(value, OVERLOAD_IS_URI_REF, null);
          }
          String addr = (String) value.value();
          return Types.boolOf(isUriRef(addr));
        });
  }

  /**
   * Creates a custom unary function overload for the "isNan" operation.
   *
   * @return The {@link Overload} instance for the "isNan" operation.
   */
  private static Overload celIsNan() {
    return Overload.unary(
        OVERLOAD_IS_NAN,
        value -> {
          if (value.type().typeEnum() != TypeEnum.Double) {
            return Err.noSuchOverload(value, OVERLOAD_IS_NAN, null);
          }
          Double doubleVal = (Double) value.value();
          return Types.boolOf(doubleVal.isNaN());
        });
  }

  /**
   * Creates a custom unary function overload for the "isInf" operation.
   *
   * @return The {@link Overload} instance for the "isInf" operation.
   */
  private static Overload celIsInf() {
    return Overload.overload(
        OVERLOAD_IS_INF,
        null,
        value -> {
          if (value.type().typeEnum() != TypeEnum.Double) {
            return Err.noSuchOverload(value, OVERLOAD_IS_INF, null);
          }
          Double doubleVal = (Double) value.value();
          return Types.boolOf(doubleVal.isInfinite());
        },
        (lhs, rhs) -> {
          if (lhs.type().typeEnum() != TypeEnum.Double || rhs.type().typeEnum() != TypeEnum.Int) {
            return Err.noSuchOverload(lhs, OVERLOAD_IS_INF, rhs);
          }
          Double value = (Double) lhs.value();
          long sign = rhs.intValue();
          if (sign == 0) {
            return Types.boolOf(value.isInfinite());
          }
          double expectedValue = (sign > 0) ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
          return Types.boolOf(value == expectedValue);
        },
        null);
  }

  private static Overload celIsHostAndPort() {
    return Overload.overload(
        OVERLOAD_IS_HOST_AND_PORT,
        null,
        null,
        (lhs, rhs) -> {
          if (lhs.type().typeEnum() != TypeEnum.String || rhs.type().typeEnum() != TypeEnum.Bool) {
            return Err.noSuchOverload(lhs, OVERLOAD_IS_HOST_AND_PORT, rhs);
          }
          String value = (String) lhs.value();
          boolean portRequired = rhs.booleanValue();
          return Types.boolOf(isHostAndPort(value, portRequired));
        },
        null);
  }

  /**
   * Returns true if the string is a valid host/port pair, for example "example.com:8080".
   *
   * <p>If the argument portRequired is true, the port is required. If the argument is false, the
   * port is optional.
   *
   * <p>The host can be one of:
   *
   * <ul>
   *   <li>An IPv4 address in dotted decimal format, for example "192.168.0.1".
   *   <li>An IPv6 address enclosed in square brackets, for example "[::1]".
   *   <li>A hostname, for example "example.com".
   * </ul>
   *
   * <p>The port is separated by a colon. It must be non-empty, with a decimal number in the range
   * of 0-65535, inclusive.
   */
  private static boolean isHostAndPort(String str, boolean portRequired) {
    if (str.isEmpty()) {
      return false;
    }

    int splitIdx = str.lastIndexOf(':');

    if (str.charAt(0) == '[') {
      int end = str.lastIndexOf(']');

      int endPlus = end + 1;
      if (endPlus == str.length()) { // no port
        return !portRequired && isIp(str.substring(1, end), 6);
      } else if (endPlus == splitIdx) { // port
        return isIp(str.substring(1, end), 6) && isPort(str.substring(splitIdx + 1));
      }
      return false; // malformed
    }

    if (splitIdx < 0) {
      return !portRequired && (isHostname(str) || isIp(str, 4));
    }

    String host = str.substring(0, splitIdx);
    String port = str.substring(splitIdx + 1);

    return ((isHostname(host) || isIp(host, 4)) && isPort(port));
  }

  // Returns true if the string is a valid port for isHostAndPort.
  private static boolean isPort(String str) {
    if (str.isEmpty()) {
      return false;
    }

    if (str.length() > 1 && str.charAt(0) == '0') {
      return false;
    }

    for (int i = 0; i < str.length(); i++) {
      char c = str.charAt(i);
      if ('0' <= c && c <= '9') {
        continue;
      }
      return false;
    }

    try {
      int val = Integer.parseInt(str);
      return val <= 65535;
    } catch (NumberFormatException nfe) {
      return false;
    }
  }

  /**
   * Determines if the input list contains unique values. If the list contains duplicate values, it
   * returns {@link BoolT#False}. If the list contains unique values, it returns {@link BoolT#True}.
   *
   * @param list The input list to check for uniqueness.
   * @return {@link BoolT#True} if the list contains unique scalar values, {@link BoolT#False}
   *     otherwise.
   */
  private static Val uniqueList(Lister list) {
    long size = list.size().intValue();
    if (size == 0) {
      return BoolT.True;
    }
    Set<Val> exist = new HashSet<>((int) size);
    Val firstVal = list.get(IntT.intOf(0));
    switch (firstVal.type().typeEnum()) {
      case Bool:
      case Int:
      case Uint:
      case Double:
      case String:
      case Bytes:
        break;
      default:
        return Err.noSuchOverload(list, OVERLOAD_UNIQUE, null);
    }
    exist.add(firstVal);
    for (int i = 1; i < size; i++) {
      Val val = list.get(IntT.intOf(i));
      if (!exist.add(val)) {
        return BoolT.False;
      }
    }
    return BoolT.True;
  }

  /**
   * isEmail returns true if addr is a valid email address.
   *
   * <p>This regex conforms to the definition for a valid email address from the HTML standard. Note
   * that this standard willfully deviates from RFC 5322, which allows many unexpected forms of
   * email addresses and will easily match a typographical error.
   *
   * @param addr The input string to validate as an email address.
   * @return {@code true} if the input string is a valid email address, {@code false} otherwise.
   */
  private static boolean isEmail(String addr) {
    return EMAIL_REGEX.matcher(addr).matches();
  }

  /**
   * Returns true if the string is a valid hostname, for example "foo.example.com".
   *
   * <p>A valid hostname follows the rules below:
   *
   * <ul>
   *   <li>The name consists of one or more labels, separated by a dot (".").
   *   <li>Each label can be 1 to 63 alphanumeric characters.
   *   <li>A label can contain hyphens ("-"), but must not start or end with a hyphen.
   *   <li>The right-most label must not be digits only.
   *   <li>The name can have a trailing dot, for example "foo.example.com.".
   *   <li>The name can be 253 characters at most, excluding the optional trailing dot.
   * </ul>
   */
  private static boolean isHostname(String val) {
    if (val.length() > 253) {
      return false;
    }

    String str;
    if (val.endsWith(".")) {
      str = val.substring(0, val.length() - 1);
    } else {
      str = val;
    }

    boolean allDigits = false;

    String[] parts = str.split("\\.", -1);

    // split hostname on '.' and validate each part
    for (String part : parts) {
      allDigits = true;

      // if part is empty, longer than 63 chars, or starts/ends with '-', it is invalid
      int len = part.length();
      if (len == 0 || len > 63 || part.startsWith("-") || part.endsWith("-")) {
        return false;
      }

      // for each character in part
      for (int i = 0; i < part.length(); i++) {
        char c = part.charAt(i);
        // if the character is not a-z, A-Z, 0-9, or '-', it is invalid
        if ((c < 'a' || c > 'z') && (c < 'A' || c > 'Z') && (c < '0' || c > '9') && c != '-') {
          return false;
        }

        allDigits = allDigits && c >= '0' && c <= '9';
      }
    }

    // the last part cannot be all numbers
    return !allDigits;
  }

  /**
   * Returns true if the string is an IPv4 or IPv6 address, optionally limited to a specific
   * version.
   *
   * <p>Version 0 means either 4 or 6. Passing a version other than 0, 4, or 6 always returns false.
   *
   * <p>IPv4 addresses are expected in the dotted decimal format, for example "192.168.5.21". IPv6
   * addresses are expected in their text representation, for example "::1", or
   * "2001:0DB8:ABCD:0012::0".
   *
   * <p>Both formats are well-defined in the internet standard RFC 3986. Zone identifiers for IPv6
   * addresses (for example "fe80::a%en1") are supported.
   */
  static boolean isIp(String addr, long ver) {
    if (ver == 6L) {
      return new Ipv6(addr).address();
    } else if (ver == 4L) {
      return new Ipv4(addr).address();
    } else if (ver == 0L) {
      return new Ipv4(addr).address() || new Ipv6(addr).address();
    }
    return false;
  }

  /**
   * Returns true if the string is a URI, for example "https://example.com/foo/bar?baz=quux#frag".
   *
   * <p>URI is defined in the internet standard RFC 3986. Zone Identifiers in IPv6 address literals
   * are supported (RFC 6874).
   */
  private static boolean isUri(String str) {
    return new Uri(str).uri();
  }

  /**
   * Returns true if the string is a URI Reference - a URI such as
   * "https://example.com/foo/bar?baz=quux#frag", or a Relative Reference such as "./foo/bar?query".
   *
   * <p>URI, URI Reference, and Relative Reference are defined in the internet standard RFC 3986.
   * Zone Identifiers in IPv6 address literals are supported (RFC 6874).
   */
  private static boolean isUriRef(String str) {
    return new Uri(str).uriReference();
  }

  /**
   * Returns true if the string is a valid IP with prefix length, optionally limited to a specific
   * version (v4 or v6), and optionally requiring the host portion to be all zeros.
   *
   * <p>An address prefix divides an IP address into a network portion, and a host portion. The
   * prefix length specifies how many bits the network portion has. For example, the IPv6 prefix
   * "2001:db8:abcd:0012::0/64" designates the left-most 64 bits as the network prefix. The range of
   * the network is 2**64 addresses, from 2001:db8:abcd:0012::0 to
   * 2001:db8:abcd:0012:ffff:ffff:ffff:ffff.
   *
   * <p>An address prefix may include a specific host address, for example
   * "2001:db8:abcd:0012::1f/64". With strict = true, this is not permitted. The host portion must
   * be all zeros, as in "2001:db8:abcd:0012::0/64".
   *
   * <p>The same principle applies to IPv4 addresses. "192.168.1.0/24" designates the first 24 bits
   * of the 32-bit IPv4 as the network prefix.
   */
  private static boolean isIpPrefix(String str, long version, boolean strict) {
    if (version == 6L) {
      Ipv6 ip = new Ipv6(str);
      return ip.addressPrefix() && (!strict || ip.isPrefixOnly());
    } else if (version == 4L) {
      Ipv4 ip = new Ipv4(str);
      return ip.addressPrefix() && (!strict || ip.isPrefixOnly());
    } else if (version == 0L) {
      return isIpPrefix(str, 6, strict) || isIpPrefix(str, 4, strict);
    }
    return false;
  }
}
