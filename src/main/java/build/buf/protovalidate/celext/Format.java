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
import org.projectnessie.cel.common.types.ref.Type;
import org.projectnessie.cel.common.types.ref.Val;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

import static org.projectnessie.cel.common.types.IntT.intOf;

public final class Format {
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    private Format() {}

    public static Val format(String fmtString, ListT list) {
        StringBuilder builder = new StringBuilder();
        int index = 0;
        int argIndex = 0;
        while (index < fmtString.length()) {
            char c = fmtString.charAt(index++);
            if (c != '%') {
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
                builder.append('%');
                index++;
                continue;
            }
            if (argIndex >= list.size().intValue()) {
                return Err.newErr("format: not enough arguments");
            }
            Val arg = list.get(intOf(argIndex++));
            c = fmtString.charAt(index++);
            int precision = 6;
            if (c == '.') {
                // parse the precision
                precision = 0;
                while (index < fmtString.length() && '0' <= fmtString.charAt(index) && fmtString.charAt(index) <= '9') {
                    precision = precision * 10 + (fmtString.charAt(index++) - '0');
                }
                if (index >= fmtString.length()) {
                    return Err.newErr("format: expected format specifier");
                }
                c = fmtString.charAt(index++);
            }

            Val status;
            switch (c) {
                case 'e':
                    status = formatExponent(builder, arg, precision);
                    break;
                case 'f':
                    status = formatFloating(builder, arg, precision);
                    break;
                case 'b':
                    status = formatBinary(builder, arg);
                    break;
                case 'o':
                    status = formatOctal(builder, arg);
                    break;
                case 'd':
                    status = formatDecimal(builder, arg);
                    break;
                case 'x':
                    status = formatHex(builder, arg, true);
                    break;
                case 'X':
                    status = formatHex(builder, arg, false);
                    break;
                case 's':
                    status = formatString(builder, arg);
                    break;
                default:
                    return Err.newErr("format: invalid format specifier");
            }
            if (status.type() == Err.ErrType) {
                return status;
            }
        }
        return StringT.stringOf(builder.toString());
    }
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static Val formatString(StringBuilder builder, Val val) {
        if (val.type() == StringT.StringType) {
            builder.append(val.value());
        } else if (val.type() == BytesT.BytesType) {
            builder.append(val.value());
        } else {
            return formatStringSafe(builder, val);
        }
        return Err.newErr("unimplemented");
    }

    private static Val formatStringSafe(StringBuilder builder, Val val) {
        Type type = val.type();
        if (type == BoolT.BoolType) {
            builder.append(val.booleanValue());
        } else if (type == IntT.IntType) {
            formatInteger(builder, Long.valueOf(val.intValue()).intValue(), 10);
        } else if (type == UintT.UintType) {
            formatUnsigned(builder, Long.valueOf(val.intValue()).byteValue(), 10);
        } else if (type == DoubleT.DoubleType) {
            builder.append(type.value());
        } else if (type == StringT.StringType) {
            builder.append("\"")
                    .append(val.value().toString())
                    .append("\"");
        } else if (type == BytesT.BytesType) {
            builder.append("\"")
                    .append(new String((byte[]) val.value(), StandardCharsets.UTF_8))
                    .append("\"");
        } else if (type == DurationT.DurationType) {
            return Err.newErr("unimplemented");
        } else if ( type == TimestampT.TimestampType) {
            return Err.newErr("unimplemented");
        } else if (type == ListT.ListType) {
            return Err.newErr("unimplemented");
        } else if (type == MapT.MapType) {
            return Err.newErr("unimplemented");
        } else if (type == NullT.NullType) {
            return Err.newErr("unimplemented");
        }
        return val;
    }

    private static void formatInteger(StringBuilder builder, int value, int base) {
        if (value < 0) {
            builder.append("-");
            value = -value;
        }
        formatUnsigned(builder, (byte) value, base);
    }

    private static Val formatHex(StringBuilder builder, Val val, boolean lowerCase) {
        String hexString;
        if (val.type() == IntT.IntType || val.type() == UintT.UintType) {
            hexString = Long.toHexString(val.intValue());
        } else if (val.type() == BytesT.BytesType) {
            byte[] bytes = (byte[])val.value();
            hexString = bytesToHex(bytes);
        } else if (val.type() == StringT.StringType) {
            hexString = val.value().toString();
        } else {
            return Err.newErr("formatHex: expected int or string");
        }

        if (lowerCase) {
            builder.append(hexString.toLowerCase(Locale.US));
        } else {
            builder.append(hexString.toUpperCase(Locale.US));
        }
        return NullT.NullType;
    }

    private static void formatHexString(StringBuilder builder, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        // Convert each byte to its hexadecimal representation
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            // Convert the byte to an unsigned integer and format it as a two-digit hexadecimal
            String hex = String.format("%02x", b & 0xFF);
            // Append the hexadecimal representation to the StringBuilder
            sb.append(hex);
        }
        builder.append(sb);
    }

    private static void formatUnsigned(StringBuilder builder, byte value, int base) {
        if (value == 0) {
            builder.append("0");
            return;
        }
        char[] buf = new char[64];
        int index = 64;
        while (value > 0 && index > 1) {
            buf[--index] = HEX_ARRAY[value % base];
            value /= base;
        }
        builder.append(Arrays.copyOfRange(buf, index - 1, buf.length - 1));
    }

    private static Val formatDecimal(StringBuilder builder, Val arg) {
        return Err.newErr("unimplemented");
    }

    private static Val formatOctal(StringBuilder builder, Val arg) {
        return Err.newErr("unimplemented");
    }

    private static Val formatBinary(StringBuilder builder, Val arg) {
        return Err.newErr("unimplemented");
    }

    private static Val formatFloating(StringBuilder builder, Val arg, int precision) {
        return Err.newErr("unimplemented");
    }

    private static Val formatExponent(StringBuilder builder, Val arg, int precision) {
        return Err.newErr("unimplemented");
    }
}
