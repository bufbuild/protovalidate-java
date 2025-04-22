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

import com.google.common.base.Ascii;
import com.google.common.base.Splitter;
import com.google.common.net.InetAddresses;
import com.google.common.primitives.Bytes;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import org.projectnessie.cel.common.types.BoolT;
import org.projectnessie.cel.common.types.Err;
import org.projectnessie.cel.common.types.IntT;
import org.projectnessie.cel.common.types.ListT;
import org.projectnessie.cel.common.types.StringT;
import org.projectnessie.cel.common.types.Types;
import org.projectnessie.cel.common.types.pb.DefaultTypeAdapter;
import org.projectnessie.cel.common.types.ref.TypeEnum;
import org.projectnessie.cel.common.types.ref.Val;
import org.projectnessie.cel.common.types.traits.Lister;
import org.projectnessie.cel.interpreter.functions.Overload;

/** Defines custom function overloads (the implementation). */
final class CustomOverload {

  private static final String OVERLOAD_GET_FIELD = "getField";
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

  /**
   * Create custom function overload list.
   *
   * @return an array of overloaded functions.
   */
  static Overload[] create() {
    return new Overload[] {
      getField(),
      format(),
      unique(),
      startsWith(),
      endsWith(),
      contains(),
      isHostname(),
      isEmail(),
      isIp(),
      isIpPrefix(),
      isUri(),
      isUriRef(),
      isNan(),
      isInf(),
      isHostAndPort(),
    };
  }

  /**
   * Creates a custom function overload for the "getField" operation.
   *
   * @return The {@link Overload} instance for the "getField" operation.
   */
  private static Overload getField() {
    return Overload.binary(
        OVERLOAD_GET_FIELD,
        (msgarg, namearg) -> {
          if (msgarg.type().typeEnum() != TypeEnum.Object
              || namearg.type().typeEnum() != TypeEnum.String) {
            return Err.newErr("no such overload");
          }
          Message message = msgarg.convertToNative(Message.class);
          String fieldName = namearg.convertToNative(String.class);
          Descriptors.FieldDescriptor field =
              message.getDescriptorForType().findFieldByName(fieldName);
          if (field == null) {
            return Err.newErr("no such field: " + fieldName);
          }
          return DefaultTypeAdapter.Instance.nativeToValue(message.getField(field));
        });
  }

  /**
   * Creates a custom binary function overload for the "format" operation.
   *
   * @return The {@link Overload} instance for the "format" operation.
   */
  private static Overload format() {
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
  private static Overload unique() {
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
  private static Overload startsWith() {
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
  private static Overload endsWith() {
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
  private static Overload contains() {
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
            return Types.boolOf(Bytes.indexOf(receiver, param) != -1);
          }
          return Err.noSuchOverload(lhs, OVERLOAD_CONTAINS, rhs);
        });
  }

  /**
   * Creates a custom binary function overload for the "isHostname" operation.
   *
   * @return The {@link Overload} instance for the "isHostname" operation.
   */
  private static Overload isHostname() {
    return Overload.unary(
        OVERLOAD_IS_HOSTNAME,
        value -> {
          if (value.type().typeEnum() != TypeEnum.String) {
            return Err.noSuchOverload(value, OVERLOAD_IS_HOSTNAME, null);
          }
          String host = (String) value.value();
          if (host.isEmpty()) {
            return BoolT.False;
          }
          return Types.boolOf(validateHostname(host));
        });
  }

  /**
   * Creates a custom unary function overload for the "isEmail" operation.
   *
   * @return The {@link Overload} instance for the "isEmail" operation.
   */
  private static Overload isEmail() {
    return Overload.unary(
        OVERLOAD_IS_EMAIL,
        value -> {
          if (value.type().typeEnum() != TypeEnum.String) {
            return Err.noSuchOverload(value, OVERLOAD_IS_EMAIL, null);
          }
          String addr = (String) value.value();
          if (addr.isEmpty()) {
            return BoolT.False;
          }
          return Types.boolOf(validateEmail(addr));
        });
  }

  /**
   * Creates a custom function overload for the "isIp" operation.
   *
   * @return The {@link Overload} instance for the "isIp" operation.
   */
  private static Overload isIp() {
    return Overload.overload(
        OVERLOAD_IS_IP,
        null,
        value -> {
          if (value.type().typeEnum() != TypeEnum.String) {
            return Err.noSuchOverload(value, OVERLOAD_IS_IP, null);
          }
          String addr = (String) value.value();
          if (addr.isEmpty()) {
            return BoolT.False;
          }
          return Types.boolOf(validateIP(addr, 0L));
        },
        (lhs, rhs) -> {
          if (lhs.type().typeEnum() != TypeEnum.String || rhs.type().typeEnum() != TypeEnum.Int) {
            return Err.noSuchOverload(lhs, OVERLOAD_IS_IP, rhs);
          }
          String address = (String) lhs.value();
          if (address.isEmpty()) {
            return BoolT.False;
          }
          return Types.boolOf(validateIP(address, rhs.intValue()));
        },
        null);
  }

  /**
   * Creates a custom function overload for the "isIpPrefix" operation.
   *
   * @return The {@link Overload} instance for the "isIpPrefix" operation.
   */
  private static Overload isIpPrefix() {
    return Overload.overload(
        OVERLOAD_IS_IP_PREFIX,
        null,
        value -> {
          if (value.type().typeEnum() != TypeEnum.String
              && value.type().typeEnum() != TypeEnum.Bool) {
            return Err.noSuchOverload(value, OVERLOAD_IS_IP_PREFIX, null);
          }
          String prefix = (String) value.value();
          if (prefix.isEmpty()) {
            return BoolT.False;
          }
          return Types.boolOf(validateIPPrefix(prefix, 0L, false));
        },
        (lhs, rhs) -> {
          if (lhs.type().typeEnum() != TypeEnum.String
              || (rhs.type().typeEnum() != TypeEnum.Int
                  && rhs.type().typeEnum() != TypeEnum.Bool)) {
            return Err.noSuchOverload(lhs, OVERLOAD_IS_IP_PREFIX, rhs);
          }
          String prefix = (String) lhs.value();
          if (prefix.isEmpty()) {
            return BoolT.False;
          }
          if (rhs.type().typeEnum() == TypeEnum.Int) {
            return Types.boolOf(validateIPPrefix(prefix, rhs.intValue(), false));
          }
          return Types.boolOf(validateIPPrefix(prefix, 0L, rhs.booleanValue()));
        },
        (values) -> {
          if (values.length != 3
              || values[0].type().typeEnum() != TypeEnum.String
              || values[1].type().typeEnum() != TypeEnum.Int
              || values[2].type().typeEnum() != TypeEnum.Bool) {
            return Err.noSuchOverload(values[0], OVERLOAD_IS_IP_PREFIX, "", values);
          }
          String prefix = (String) values[0].value();
          if (prefix.isEmpty()) {
            return BoolT.False;
          }
          return Types.boolOf(
              validateIPPrefix(prefix, values[1].intValue(), values[2].booleanValue()));
        });
  }

  /**
   * Creates a custom unary function overload for the "isUri" operation.
   *
   * @return The {@link Overload} instance for the "isUri" operation.
   */
  private static Overload isUri() {
    return Overload.unary(
        OVERLOAD_IS_URI,
        value -> {
          if (value.type().typeEnum() != TypeEnum.String) {
            return Err.noSuchOverload(value, OVERLOAD_IS_URI, null);
          }
          String addr = (String) value.value();
          if (addr.isEmpty()) {
            return BoolT.False;
          }
          return Types.boolOf(validateURI(addr, true));
        });
  }

  /**
   * Creates a custom unary function overload for the "isUriRef" operation.
   *
   * @return The {@link Overload} instance for the "isUriRef" operation.
   */
  private static Overload isUriRef() {
    return Overload.unary(
        OVERLOAD_IS_URI_REF,
        value -> {
          if (value.type().typeEnum() != TypeEnum.String) {
            return Err.noSuchOverload(value, OVERLOAD_IS_URI_REF, null);
          }
          String addr = (String) value.value();
          if (addr.isEmpty()) {
            return BoolT.False;
          }
          return Types.boolOf(validateURI(addr, false));
        });
  }

  /**
   * Creates a custom unary function overload for the "isNan" operation.
   *
   * @return The {@link Overload} instance for the "isNan" operation.
   */
  private static Overload isNan() {
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
  private static Overload isInf() {
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

  private static Overload isHostAndPort() {
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
          return Types.boolOf(hostAndPort(value, portRequired));
        },
        null);
  }

  private static boolean hostAndPort(String value, boolean portRequired) {
    if (value.isEmpty()) {
      return false;
    }
    int splitIdx = value.lastIndexOf(':');
    if (value.charAt(0) == '[') { // ipv6
      int end = value.indexOf(']');
      if (end + 1 == value.length()) { // no port
        return !portRequired && validateIP(value.substring(1, end), 6);
      }
      if (end + 1 == splitIdx) { // port
        return validateIP(value.substring(1, end), 6)
            && validatePort(value.substring(splitIdx + 1));
      }
      return false; // malformed
    }
    if (splitIdx < 0) {
      return !portRequired && (validateHostname(value) || validateIP(value, 4));
    }
    String host = value.substring(0, splitIdx);
    String port = value.substring(splitIdx + 1);
    return (validateHostname(host) || validateIP(host, 4)) && validatePort(port);
  }

  private static boolean validatePort(String value) {
    try {
      int portNum = Integer.parseInt(value);
      return portNum >= 0 && portNum <= 65535;
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
   * Validates if the input string is a valid email address.
   *
   * @param addr The input string to validate as an email address.
   * @return {@code true} if the input string is a valid email address, {@code false} otherwise.
   */
  private static boolean validateEmail(String addr) {
    try {
      InternetAddress emailAddr = new InternetAddress(addr);
      emailAddr.validate();
      if (addr.contains("<") || !emailAddr.getAddress().equals(addr)) {
        return false;
      }
      addr = emailAddr.getAddress();
      if (addr.length() > 254) {
        return false;
      }
      String[] parts = addr.split("@", 2);
      return parts[0].length() < 64 && validateHostname(parts[1]);
    } catch (AddressException ex) {
      return false;
    }
  }

  /**
   * Validates if the input string is a valid hostname.
   *
   * @param host The input string to validate as a hostname.
   * @return {@code true} if the input string is a valid hostname, {@code false} otherwise.
   */
  private static boolean validateHostname(String host) {
    if (host.length() > 253) {
      return false;
    }
    String s = Ascii.toLowerCase(host.endsWith(".") ? host.substring(0, host.length() - 1) : host);
    Iterable<String> parts = Splitter.on('.').split(s);
    boolean allDigits = false;
    for (String part : parts) {
      allDigits = true;
      int l = part.length();
      if (l == 0 || l > 63 || part.charAt(0) == '-' || part.charAt(l - 1) == '-') {
        return false;
      }
      for (int i = 0; i < l; i++) {
        char ch = part.charAt(i);
        if (!Ascii.isLowerCase(ch) && !isDigit(ch) && ch != '-') {
          return false;
        }
        allDigits = allDigits && isDigit(ch);
      }
    }
    // the last part cannot be all numbers
    return !allDigits;
  }

  private static boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  /**
   * Validates if the input string is a valid IP address.
   *
   * @param addr The input string to validate as an IP address.
   * @param ver The IP version to validate against (0 for any version, 4 for IPv4, 6 for IPv6).
   * @return {@code true} if the input string is a valid IP address of the specified version, {@code
   *     false} otherwise.
   */
  private static boolean validateIP(String addr, long ver) {
    InetAddress address;
    try {
      address = InetAddresses.forString(addr);
    } catch (Exception e) {
      return false;
    }
    if (ver == 0L) {
      return true;
    } else if (ver == 4L) {
      return address instanceof Inet4Address;
    } else if (ver == 6L) {
      return address instanceof Inet6Address;
    }
    return false;
  }

  /**
   * Validates if the input string is a valid URI, which can be a URL or a URN.
   *
   * @param val The input string to validate as a URI.
   * @param checkAbsolute Whether to check if this URI is absolute (i.e. has a scheme component)
   * @return {@code true} if the input string is a valid URI, {@code false} otherwise.
   */
  private static boolean validateURI(String val, boolean checkAbsolute) {
    try {
      URI uri = new URI(val);
      if (checkAbsolute) {
        return uri.isAbsolute();
      }
      return true;
    } catch (URISyntaxException e) {
      return false;
    }
  }

  /**
   * Validates if the input string is a valid IP prefix.
   *
   * @param prefix The input string to validate as an IP prefix.
   * @param ver The IP version to validate against (0 for any version, 4 for IPv4, 6 for IPv6).
   * @param strict If strict is true and host bits are set in the supplied address, then false is
   *     returned.
   * @return {@code true} if the input string is a valid IP prefix of the specified version, {@code
   *     false} otherwise.
   */
  private static boolean validateIPPrefix(String prefix, long ver, boolean strict) {
    IPAddressString str;
    IPAddress addr;
    try {
      str = new IPAddressString(prefix);
      addr = str.toAddress();
    } catch (Exception e) {
      return false;
    }
    if (!addr.isPrefixed()) {
      return false;
    }
    if (strict) {
      IPAddress mask = addr.getNetworkMask().withoutPrefixLength();
      if (!addr.mask(mask).equals(str.getHostAddress())) {
        return false;
      }
    }
    if (ver == 0L) {
      return true;
    } else if (ver == 4L) {
      return addr.isIPv4();
    } else if (ver == 6L) {
      return addr.isIPv6();
    }
    return false;
  }
}
