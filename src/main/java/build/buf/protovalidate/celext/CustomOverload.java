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

package build.buf.protovalidate.celext;

import static org.projectnessie.cel.common.types.IntT.intOf;
import static org.projectnessie.cel.interpreter.functions.Overload.binary;
import static org.projectnessie.cel.interpreter.functions.Overload.overload;
import static org.projectnessie.cel.interpreter.functions.Overload.unary;

import com.google.common.net.InetAddresses;
import com.google.common.primitives.Bytes;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
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

final class CustomOverload {
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
      isUriRef()
    };
  }

  private static Overload binaryFormat() {
    return binary(
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

  private static Overload unaryUnique() {
    return unary(
        "unique",
        (val) -> {
          switch (val.type().typeEnum()) {
            case List:
              Lister lister = (Lister) val;
              if (lister.size().intValue() == 0L) {
                // Uniqueness for empty lists are true.
                return BoolT.True;
              }
              Val firstValue = lister.get(intOf(0));
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

  private static Overload binaryStartsWith() {
    return binary(
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

  private static Overload binaryEndsWith() {
    return binary(
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

  private static Overload binaryContains() {
    return binary(
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

  private static Overload binaryIsHostname() {
    return unary(
        "isHostname",
        value -> {
          String host = value.value().toString();
          if (host.isEmpty()) {
            return BoolT.False;
          }
          return Types.boolOf(validateHostname(host));
        });
  }

  private static Overload unaryIsEmail() {
    return unary(
        "isEmail",
        value -> {
          String addr = value.value().toString();
          if (addr.isEmpty()) {
            return BoolT.False;
          }
          return Types.boolOf(validateEmail(addr));
        });
  }

  private static Overload isIp() {
    return overload(
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

  private static Overload isUri() {
    return unary(
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

  private static Overload isUriRef() {
    return unary(
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

  private static Val uniqueBytes(Lister list) {
    Set<Object> exist = new HashSet<>();
    for (int i = 0; i < list.size().intValue(); i++) {
      Object val = list.get(intOf(i)).value();
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

  private static Val uniqueScalar(Lister list) {
    Set<Val> exist = new HashSet<>();
    for (int i = 0; i < list.size().intValue(); i++) {
      Val val = list.get(intOf(i));
      if (exist.contains(val)) {
        return BoolT.False;
      }
      exist.add(val);
    }
    return BoolT.True;
  }

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

  private static boolean validateHostname(String host) {
    if (host.length() > 253) {
      return false;
    }

    String s = host.toLowerCase().replaceAll("\\.$", "");
    String[] parts = s.split("\\.");

    for (String part : parts) {
      int l = part.length();
      if (l == 0 || l > 63 || part.charAt(0) == '-' || part.charAt(l - 1) == '-') {
        return false;
      }

      for (char ch : part.toCharArray()) {
        if ((ch < 'a' || ch > 'z') && (ch < '0' || ch > '9') && ch != '-') {
          return false;
        }
      }
    }

    return true;
  }

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
