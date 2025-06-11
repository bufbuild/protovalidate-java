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

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import dev.cel.common.CelErrorCode;
import dev.cel.common.CelRuntimeException;
import dev.cel.common.types.CelType;
import dev.cel.common.types.SimpleType;
import dev.cel.runtime.CelEvaluationException;
import dev.cel.runtime.CelRuntime.CelFunctionBinding;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Defines custom function overloads (the implementation). */
final class CustomOverload {

  // See https://html.spec.whatwg.org/multipage/input.html#valid-e-mail-address
  private static final Pattern EMAIL_REGEX =
      Pattern.compile(
          "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$");

  /**
   * Create custom function overload list.
   *
   * @return a list of overloaded functions.
   */
  static List<CelFunctionBinding> create() {
    ArrayList<CelFunctionBinding> bindings = new ArrayList<>();
    bindings.addAll(
        Arrays.asList(
            celBytesToString(),
            celGetField(),
            celFormat(),
            celStartsWithBytes(),
            celEndsWithBytes(),
            celContainsBytes(),
            celIsHostname(),
            celIsEmail(),
            celIsIpUnary(),
            celIsIp(),
            celIsIpPrefix(),
            celIsIpPrefixInt(),
            celIsIpPrefixBool(),
            celIsIpPrefixIntBool(),
            celIsUri(),
            celIsUriRef(),
            celIsNan(),
            celIsInfUnary(),
            celIsInfBinary(),
            celIsHostAndPort()));
    bindings.addAll(celUnique());
    return Collections.unmodifiableList(bindings);
  }

  /**
   * This implements that standard {@code bytes_to_string} function. We override it because the CEL
   * library doesn't validate that the bytes are valid utf-8.
   *
   * <p>Workaround until <a href="https://github.com/google/cel-java/pull/717">cel-java issue
   * 717</a> lands.
   */
  private static CelFunctionBinding celBytesToString() {
    return CelFunctionBinding.from(
        "bytes_to_string",
        ByteString.class,
        v -> {
          if (!v.isValidUtf8()) {
            throw new CelRuntimeException(
                new IllegalArgumentException("invalid UTF-8 in bytes, cannot convert to string"),
                CelErrorCode.BAD_FORMAT);
          }
          return v.toStringUtf8();
        });
  }

  /**
   * Creates a custom function overload for the "getField" operation.
   *
   * @return The {@link CelFunctionBinding} instance for the "getField" operation.
   */
  private static CelFunctionBinding celGetField() {
    return CelFunctionBinding.from(
        "get_field_any_string",
        Message.class,
        String.class,
        (message, fieldName) -> {
          Descriptors.FieldDescriptor field =
              message.getDescriptorForType().findFieldByName(fieldName);
          if (field == null) {
            throw new CelEvaluationException("no such field: " + fieldName);
          }
          return ProtoAdapter.toCel(field, message.getField(field));
        });
  }

  /**
   * Creates a custom binary function overload for the "format" operation.
   *
   * @return The {@link CelFunctionBinding} instance for the "format" operation.
   */
  private static CelFunctionBinding celFormat() {
    return CelFunctionBinding.from("format_list_dyn", String.class, List.class, Format::format);
  }

  /**
   * Creates a custom unary function overload for the "unique" operation.
   *
   * @return The {@link CelFunctionBinding} instance for the "unique" operation.
   */
  private static List<CelFunctionBinding> celUnique() {
    List<CelFunctionBinding> uniqueOverloads = new ArrayList<>();
    for (CelType type :
        Arrays.asList(
            SimpleType.STRING,
            SimpleType.INT,
            SimpleType.UINT,
            SimpleType.DOUBLE,
            SimpleType.BYTES,
            SimpleType.BOOL)) {
      uniqueOverloads.add(
          CelFunctionBinding.from(
              String.format("unique_list_%s", type.name().toLowerCase(Locale.US)),
              List.class,
              CustomOverload::uniqueList));
    }
    return Collections.unmodifiableList(uniqueOverloads);
  }

  /**
   * Creates a custom binary function overload for the "startsWith" operation.
   *
   * @return The {@link CelFunctionBinding} instance for the "startsWith" operation.
   */
  private static CelFunctionBinding celStartsWithBytes() {
    return CelFunctionBinding.from(
        "starts_with_bytes",
        ByteString.class,
        ByteString.class,
        (receiver, param) -> {
          if (receiver.size() < param.size()) {
            return false;
          }
          for (int i = 0; i < param.size(); i++) {
            if (param.byteAt(i) != receiver.byteAt(i)) {
              return false;
            }
          }
          return true;
        });
  }

  /**
   * Creates a custom binary function overload for the "endsWith" operation.
   *
   * @return The {@link CelFunctionBinding} instance for the "endsWith" operation.
   */
  private static CelFunctionBinding celEndsWithBytes() {
    return CelFunctionBinding.from(
        "ends_with_bytes",
        ByteString.class,
        ByteString.class,
        (receiver, param) -> {
          if (receiver.size() < param.size()) {
            return false;
          }
          for (int i = 0; i < param.size(); i++) {
            if (param.byteAt(param.size() - i - 1) != receiver.byteAt(receiver.size() - i - 1)) {
              return false;
            }
          }
          return true;
        });
  }

  /**
   * Creates a custom binary function overload for the "contains" operation.
   *
   * @return The {@link CelFunctionBinding} instance for the "contains" operation.
   */
  private static CelFunctionBinding celContainsBytes() {
    return CelFunctionBinding.from(
        "contains_bytes",
        ByteString.class,
        ByteString.class,
        (receiver, param) -> {
          return bytesContains(receiver.toByteArray(), param.toByteArray());
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
   * @return The {@link CelFunctionBinding} instance for the "isHostname" operation.
   */
  private static CelFunctionBinding celIsHostname() {
    return CelFunctionBinding.from("is_hostname", String.class, CustomOverload::isHostname);
  }

  /**
   * Creates a custom unary function overload for the "isEmail" operation.
   *
   * @return The {@link CelFunctionBinding} instance for the "isEmail" operation.
   */
  private static CelFunctionBinding celIsEmail() {
    return CelFunctionBinding.from("is_email", String.class, CustomOverload::isEmail);
  }

  /**
   * Creates a custom function overload for the "isIp" operation.
   *
   * @return The {@link CelFunctionBinding} instance for the "isIp" operation.
   */
  private static CelFunctionBinding celIsIpUnary() {
    return CelFunctionBinding.from("is_ip_unary", String.class, value -> isIp(value, 0L));
  }

  /**
   * Creates a custom function overload for the "isIp" operation that also accepts a port.
   *
   * @return The {@link CelFunctionBinding} instance for the "isIp" operation.
   */
  private static CelFunctionBinding celIsIp() {
    return CelFunctionBinding.from("is_ip", String.class, Long.class, CustomOverload::isIp);
  }

  /**
   * Creates a custom function overload for the "isIpPrefix" operation.
   *
   * @return The {@link CelFunctionBinding} instance for the "isIpPrefix" operation.
   */
  private static CelFunctionBinding celIsIpPrefix() {
    return CelFunctionBinding.from(
        "is_ip_prefix", String.class, prefix -> isIpPrefix(prefix, 0L, false));
  }

  /**
   * Creates a custom function overload for the "isIpPrefix" operation that accepts a version.
   *
   * @return The {@link CelFunctionBinding} instance for the "isIpPrefix" operation.
   */
  private static CelFunctionBinding celIsIpPrefixInt() {
    return CelFunctionBinding.from(
        "is_ip_prefix_int",
        String.class,
        Long.class,
        (prefix, version) -> {
          return isIpPrefix(prefix, version, false);
        });
  }

  /**
   * Creates a custom function overload for the "isIpPrefix" operation that accepts a strict flag.
   *
   * @return The {@link CelFunctionBinding} instance for the "isIpPrefix" operation.
   */
  private static CelFunctionBinding celIsIpPrefixBool() {
    return CelFunctionBinding.from(
        "is_ip_prefix_bool",
        String.class,
        Boolean.class,
        (prefix, strict) -> {
          return isIpPrefix(prefix, 0L, strict);
        });
  }

  /**
   * Creates a custom function overload for the "isIpPrefix" operation that accepts both version and
   * strict flag.
   *
   * @return The {@link CelFunctionBinding} instance for the "isIpPrefix" operation.
   */
  private static CelFunctionBinding celIsIpPrefixIntBool() {
    return CelFunctionBinding.from(
        "is_ip_prefix_int_bool",
        Arrays.asList(String.class, Long.class, Boolean.class),
        (args) -> isIpPrefix((String) args[0], (Long) args[1], (Boolean) args[2]));
  }

  /**
   * Creates a custom unary function overload for the "isUri" operation.
   *
   * @return The {@link CelFunctionBinding} instance for the "isUri" operation.
   */
  private static CelFunctionBinding celIsUri() {
    return CelFunctionBinding.from("is_uri", String.class, CustomOverload::isUri);
  }

  /**
   * Creates a custom unary function overload for the "isUriRef" operation.
   *
   * @return The {@link CelFunctionBinding} instance for the "isUriRef" operation.
   */
  private static CelFunctionBinding celIsUriRef() {
    return CelFunctionBinding.from("is_uri_ref", String.class, CustomOverload::isUriRef);
  }

  /**
   * Creates a custom unary function overload for the "isNan" operation.
   *
   * @return The {@link CelFunctionBinding} instance for the "isNan" operation.
   */
  private static CelFunctionBinding celIsNan() {
    return CelFunctionBinding.from("is_nan", Double.class, value -> Double.isNaN(value));
  }

  /**
   * Creates a custom unary function overload for the "isInf" operation.
   *
   * @return The {@link CelFunctionBinding} instance for the "isInf" operation.
   */
  private static CelFunctionBinding celIsInfUnary() {
    return CelFunctionBinding.from("is_inf_unary", Double.class, value -> value.isInfinite());
  }

  /**
   * Creates a custom unary function overload for the "isInf" operation with sign option.
   *
   * @return The {@link CelFunctionBinding} instance for the "isInf" operation.
   */
  private static CelFunctionBinding celIsInfBinary() {
    return CelFunctionBinding.from(
        "is_inf_binary",
        Double.class,
        Long.class,
        (value, sign) -> {
          if (sign == 0) {
            return value.isInfinite();
          }
          double expectedValue = (sign > 0) ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
          return value == expectedValue;
        });
  }

  private static CelFunctionBinding celIsHostAndPort() {
    return CelFunctionBinding.from(
        "string_bool_is_host_and_port_bool",
        String.class,
        Boolean.class,
        CustomOverload::isHostAndPort);
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
   *   <li>An IPv4 address in dotted decimal format, for example {@code 192.168.0.1}.
   *   <li>An IPv6 address enclosed in square brackets, for example {@code [::1]}.
   *   <li>A hostname, for example {@code example.com}.
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
   * returns {@code false}. If the list contains unique values, it returns {@code true}.
   *
   * @param list The input list to check for uniqueness.
   * @return {@code true} if the list contains unique scalar values, {@code false} otherwise.
   */
  private static boolean uniqueList(List<?> list) throws CelEvaluationException {
    long size = list.size();
    if (size == 0) {
      return true;
    }
    Set<Object> exist = new HashSet<>((int) size);
    for (Object val : list) {
      if (!exist.add(val)) {
        return false;
      }
    }
    return true;
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

      // if part is empty, longer than 63 chars, or starts/ends with '-', it is
      // invalid
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
   * Returns true if the string is a URI, for example {@code
   * https://example.com/foo/bar?baz=quux#frag}.
   *
   * <p>URI is defined in the internet standard RFC 3986. Zone Identifiers in IPv6 address literals
   * are supported (RFC 6874).
   */
  private static boolean isUri(String str) {
    return new Uri(str).uri();
  }

  /**
   * Returns true if the string is a URI Reference - a URI such as {@code
   * https://example.com/foo/bar?baz=quux#frag}, or a Relative Reference such as {@code
   * ./foo/bar?query}.
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
