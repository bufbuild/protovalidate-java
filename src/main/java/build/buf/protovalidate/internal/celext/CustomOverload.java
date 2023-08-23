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

import com.google.common.base.Ascii;
import com.google.common.base.Splitter;
import com.google.common.net.InetAddresses;
import com.google.common.primitives.Bytes;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import org.projectnessie.cel.common.types.BoolT;
import org.projectnessie.cel.common.types.BytesT;
import org.projectnessie.cel.common.types.DoubleT;
import org.projectnessie.cel.common.types.Err;
import org.projectnessie.cel.common.types.IntT;
import org.projectnessie.cel.common.types.ListT;
import org.projectnessie.cel.common.types.StringT;
import org.projectnessie.cel.common.types.Types;
import org.projectnessie.cel.common.types.UintT;
import org.projectnessie.cel.common.types.ref.Val;
import org.projectnessie.cel.common.types.traits.Lister;
import org.projectnessie.cel.interpreter.functions.Overload;
import org.projectnessie.cel.interpreter.functions.UnaryOp;

/** Defines custom function overloads (the implementation). */
final class CustomOverload {
  /**
   * Create custom function overload list.
   *
   * @return an array of overloaded functions.
   */
  static Overload[] create() {
    return new Overload[] {
      binaryFormat(),
      unaryUnique(),
      binaryStartsWith(),
      binaryEndsWith(),
      binaryContains(),
      binaryIsHostname(),
      unaryIsEmail(),
      isIp(),
      isUri(),
      isUriRef(),
      isNan(),
    };
  }

  /**
   * Creates a custom binary function overload for the "format" operation.
   *
   * @return The {@link Overload} instance for the "format" operation.
   */
  private static Overload binaryFormat() {
    return Overload.binary(
        "format",
        (lhs, rhs) -> {
          if (rhs.type() != ListT.ListType) {
            return Err.newErr("format: expected list");
          }
          ListT list = (ListT) rhs.convertToType(ListT.ListType);
          String formatString = lhs.value().toString();
          Val status = Format.format(formatString, list);
          if (status.type() == Err.ErrType) {
            return status;
          }
          return StringT.stringOf(status.value().toString());
        });
  }

  /**
   * Creates a custom unary function overload for the "unique" operation.
   *
   * @return The {@link Overload} instance for the "unique" operation.
   */
  private static Overload unaryUnique() {
    return Overload.unary(
        "unique",
        (val) -> {
          switch (val.type().typeEnum()) {
            case List:
              Lister lister = (Lister) val;
              if (lister.size().intValue() == 0L) {
                // Uniqueness for empty lists are true.
                return BoolT.True;
              }
              Val firstValue = lister.get(IntT.intOf(0));
              return unaryOpForPrimitiveVal(firstValue).invoke(lister);
            case Bool:
            case Bytes:
            case Double:
            case Int:
            case String:
            case Uint:
              return unaryOpForPrimitiveVal(val).invoke(val);
            default:
              return Err.maybeNoSuchOverloadErr(val);
          }
        });
  }

  /**
   * Creates a custom binary function overload for the "startsWith" operation.
   *
   * @return The {@link Overload} instance for the "startsWith" operation.
   */
  private static Overload binaryStartsWith() {
    return Overload.binary(
        "startsWith",
        (lhs, rhs) -> {
          if (lhs.type() == StringT.StringType && rhs.type() == StringT.StringType) {
            String receiver = lhs.value().toString();
            String param = rhs.value().toString();
            return receiver.startsWith(param) ? BoolT.True : BoolT.False;
          } else if (lhs.type() == BytesT.BytesType && rhs.type() == BytesT.BytesType) {
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
          return Err.newErr("using startsWith on a non-byte and non-string type");
        });
  }

  /**
   * Creates a custom binary function overload for the "endsWith" operation.
   *
   * @return The {@link Overload} instance for the "endsWith" operation.
   */
  private static Overload binaryEndsWith() {
    return Overload.binary(
        "endsWith",
        (lhs, rhs) -> {
          if (lhs.type() == StringT.StringType && rhs.type() == StringT.StringType) {
            String receiver = lhs.value().toString();
            String param = rhs.value().toString();
            return receiver.endsWith(param) ? BoolT.True : BoolT.False;
          } else if (lhs.type() == BytesT.BytesType && rhs.type() == BytesT.BytesType) {
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
          return Err.newErr("using endsWith on a non-byte and non-string type");
        });
  }

  /**
   * Creates a custom binary function overload for the "contains" operation.
   *
   * @return The {@link Overload} instance for the "contains" operation.
   */
  private static Overload binaryContains() {
    return Overload.binary(
        "contains",
        (lhs, rhs) -> {
          if (lhs.type() == StringT.StringType && rhs.type() == StringT.StringType) {
            String receiver = lhs.value().toString();
            String param = rhs.value().toString();
            return receiver.contains(param) ? BoolT.True : BoolT.False;
          } else if (lhs.type() == BytesT.BytesType && rhs.type() == BytesT.BytesType) {
            byte[] receiver = (byte[]) lhs.value();
            byte[] param = (byte[]) rhs.value();
            return Bytes.indexOf(receiver, param) == -1 ? BoolT.False : BoolT.True;
          }
          return Err.newErr("using contains on a non-byte and non-string type");
        });
  }

  /**
   * Creates a custom binary function overload for the "isHostname" operation.
   *
   * @return The {@link Overload} instance for the "isHostname" operation.
   */
  private static Overload binaryIsHostname() {
    return Overload.unary(
        "isHostname",
        value -> {
          String host = value.value().toString();
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
  private static Overload unaryIsEmail() {
    return Overload.unary(
        "isEmail",
        value -> {
          String addr = value.value().toString();
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
        "isIp",
        null,
        value -> {
          String addr = value.value().toString();
          if (addr.isEmpty()) {
            return BoolT.False;
          }
          return Types.boolOf(validateIP(addr, 0L));
        },
        (lhs, rhs) -> {
          String address = lhs.value().toString();
          if (address.isEmpty()) {
            return BoolT.False;
          }
          return Types.boolOf(validateIP(address, rhs.intValue()));
        },
        null);
  }

  /**
   * Creates a custom unary function overload for the "isUri" operation.
   *
   * @return The {@link Overload} instance for the "isUri" operation.
   */
  private static Overload isUri() {
    return Overload.unary(
        "isUri",
        value -> {
          String addr = value.value().toString();
          if (addr.isEmpty()) {
            return BoolT.False;
          }
          try {
            return Types.boolOf(new URL(addr).toURI().isAbsolute());
          } catch (MalformedURLException | URISyntaxException e) {
            return BoolT.False;
          }
        });
  }

  /**
   * Creates a custom unary function overload for the "isUriRef" operation.
   *
   * @return The {@link Overload} instance for the "isUriRef" operation.
   */
  private static Overload isUriRef() {
    return Overload.unary(
        "isUriRef",
        value -> {
          String addr = value.value().toString();
          if (addr.isEmpty()) {
            return BoolT.False;
          }
          try {
            // TODO: The URL api requires a host or it always fails.
            String host = "http://protovalidate.buf.build";
            URL url = new URL(host + addr);
            return url.getPath() != null && !url.getPath().isEmpty() ? BoolT.True : BoolT.False;
          } catch (MalformedURLException e) {
            return BoolT.False;
          }
        });
  }

  /**
   * Creates a custom unary function overload for the "isNan" operation.
   *
   * @return The {@link Overload} instance for the "isNan" operation.
   */
  private static Overload isNan() {
    return Overload.unary(
        "isNan",
        value -> value.convertToNative(Double.TYPE).isNaN() ? BoolT.True : BoolT.False);
  }

  /**
   * Creates a custom unary function overload for the "isInf" operation.
   *
   * @return The {@link Overload} instance for the "isInf" operation.
   */
  private static Overload isInf() {
    return Overload.unary(
        "isNan",
        value -> value.convertToNative(Double.TYPE).isInfinite() ? BoolT.True : BoolT.False);
  }

  /**
   * Retrieves the appropriate unary operation for a primitive value based on its type. This method
   * returns the unary operation that should be applied to the given primitive value.
   *
   * @param val The primitive value for which to retrieve the unary operation.
   * @return The {@link UnaryOp} instance representing the appropriate unary operation for the
   *     value.
   * @throws IllegalArgumentException if the value's type is not supported.
   */
  private static UnaryOp unaryOpForPrimitiveVal(Val val) {
    switch (val.type().typeEnum()) {
      case Bool:
        return uniqueMemberOverload(BoolT.BoolType, CustomOverload::uniqueScalar);
      case Bytes:
        return uniqueMemberOverload(BytesT.BytesType, CustomOverload::uniqueBytes);
      case Double:
        return uniqueMemberOverload(DoubleT.DoubleType, CustomOverload::uniqueScalar);
      case Int:
        return uniqueMemberOverload(IntT.IntType, CustomOverload::uniqueScalar);
      case String:
        return uniqueMemberOverload(StringT.StringType, CustomOverload::uniqueScalar);
      case Uint:
        return uniqueMemberOverload(UintT.UintType, CustomOverload::uniqueScalar);
      default:
        return Err::maybeNoSuchOverloadErr;
    }
  }

  /**
   * Creates a custom unary operation overload for processing list values with a specific item type.
   * The overload ensures that the list contains unique values of the specified item type.
   *
   * @param itemType The type of items expected in the list.
   * @param overload The function to be invoked on the unique values.
   * @return The {@link UnaryOp} instance for the unique member overload.
   */
  private static UnaryOp uniqueMemberOverload(
      org.projectnessie.cel.common.types.ref.Type itemType, overloadFunc overload) {
    return value -> {
      Lister list = (Lister) value;
      if (list == null || list.size().intValue() == 0L) {
        return Err.noMoreElements();
      }
      Val firstValue = list.get(IntT.intOf(0));
      if (firstValue.type() != itemType) {
        return Err.newTypeConversionError(list.type(), itemType);
      }
      return overload.invoke(list);
    };
  }

  @FunctionalInterface
  private interface overloadFunc {
    Val invoke(Lister list);
  }

  /**
   * Determines if the input list of bytes contains unique elements. If the list contains duplicate
   * byte arrays or strings, it returns false. If the list contains unique byte arrays or strings,
   * it returns true.
   *
   * @param list The input list to check for uniqueness.
   * @return {@link BoolT}.True if the list contains unique elements, {@link BoolT}.False otherwise.
   */
  private static Val uniqueBytes(Lister list) {
    Set<Object> exist = new HashSet<>();
    for (int i = 0; i < list.size().intValue(); i++) {
      Object val = list.get(IntT.intOf(i)).value();
      if (val instanceof byte[]) {
        val = new String((byte[]) val, StandardCharsets.UTF_8);
      }
      if (exist.contains(val)) {
        return BoolT.False;
      }
      exist.add(val.toString());
    }
    return BoolT.True;
  }

  /**
   * Determines if the input list contains unique scalar values. If the list contains duplicate
   * scalar values, it returns {@link BoolT}.False. If the list contains unique scalar values, it
   * returns {@link BoolT}.True.
   *
   * @param list The input list to check for uniqueness.
   * @return {@link BoolT}.True if the list contains unique scalar values, {@link BoolT}.False
   *     otherwise.
   */
  private static Val uniqueScalar(Lister list) {
    Set<Val> exist = new HashSet<>();
    long size = list.size().intValue();
    for (int i = 0; i < size; i++) {
      Val val = list.get(IntT.intOf(i));
      if (exist.contains(val)) {
        return BoolT.False;
      }
      exist.add(val);
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
      if (addr.contains("<")) {
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

    for (String part : parts) {
      int l = part.length();
      if (l == 0 || l > 63 || part.charAt(0) == '-' || part.charAt(l - 1) == '-') {
        return false;
      }

      for (int i = 0; i < part.length(); i++) {
        char ch = part.charAt(i);
        if ((ch < 'a' || ch > 'z') && (ch < '0' || ch > '9') && ch != '-') {
          return false;
        }
      }
    }

    return true;
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
}
