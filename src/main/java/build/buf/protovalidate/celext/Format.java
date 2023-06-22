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

import build.buf.protovalidate.errors.RuntimeError;
import com.google.common.base.Strings;
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.projectnessie.cel.common.types.IntT.intOf;
import static org.projectnessie.cel.common.types.pb.DefaultTypeAdapter.nativeToValue;

public final class Format {
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    private static final char[] LOWER_HEX_ARRAY = "0123456789abcdef".toCharArray();

    private Format() {
    }

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
                    status = formatHex(builder, arg, LOWER_HEX_ARRAY);
                    break;
                case 'X':
                    status = formatHex(builder, arg, HEX_ARRAY);
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

    public static String bytesToHex(byte[] bytes, char[] digits) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = digits[v >>> 4];
            hexChars[j * 2 + 1] = digits[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static Val formatString(StringBuilder builder, Val val) {
        if (val.type() == StringT.StringType) {
            builder.append(val.value());
            return NullT.NullValue;
        } else if (val.type() == BytesT.BytesType) {
            builder.append(val.value());
            return NullT.NullValue;
        } else {
            return formatStringSafe(builder, val);
        }
    }

    private static Val formatStringSafe(StringBuilder builder, Val val) {
        Type type = val.type();
        if (type == BoolT.BoolType) {
            builder.append(val.booleanValue());
        } else if (type == IntT.IntType) {
            formatInteger(builder, Long.valueOf(val.intValue()).intValue());
        } else if (type == UintT.UintType) {
            formatInteger(builder, Long.valueOf(val.intValue()).intValue());
        } else if (type == DoubleT.DoubleType) {
            builder.append(val.value());
        } else if (type == StringT.StringType) {
            builder.append("\"")
                    .append(val.value().toString())
                    .append("\"");
        } else if (type == BytesT.BytesType) {
            builder.append("\"")
                    .append(new String((byte[]) val.value(), StandardCharsets.UTF_8))
                    .append("\"");
        } else if (type == DurationT.DurationType) {
            builder.append("duration(");
            Duration duration = val.convertToNative(Duration.class);
            builder.append(duration.getSeconds());
            builder.append('s');
            builder.append(")");
        } else if (type == TimestampT.TimestampType) {
            builder.append("timestamp(");
            Timestamp timestamp = val.convertToNative(Timestamp.class);
            builder.append(timestamp.toString());
            builder.append(")");
        } else if (type == ListT.ListType) {
            builder.append('[');
            List list = val.convertToNative(List.class);
            for (int i = 0; i < list.size(); i++) {
                Object obj = list.get(i);
                formatStringSafe(builder, nativeToValue(Db.newDb(), null, obj));
                if (i != list.size() - 1) {
                    builder.append(", ");
                }
            }
            builder.append(']');
        } else if (type == MapT.MapType) {
            throw RuntimeError.newRuntimeErrorf("unimplemented stringSafe map type");
        } else if (type == NullT.NullType) {
            throw RuntimeError.newRuntimeErrorf("unimplemented stringSafe null type");
        }
        return val;
    }

    private static void formatInteger(StringBuilder builder, int value) {
        if (value < 0) {
            builder.append("-");
            value = -value;
        }
        builder.append(value);
    }

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
            throw RuntimeError.newRuntimeErrorf("formatHex: expected int or string");
        }
        builder.append(hexString);
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


    private static void formatUnsigned(StringBuilder builder, int value, int base) {
        formatUnsigned(builder, value, base, LOWER_HEX_ARRAY);
    }

    private static void formatUnsigned(StringBuilder builder, int value, int base, char[] digits) {
        if (value == 0) {
            builder.append("0");
            return;
        }
        char[] buf = new char[64];
        int index = 64;
        while (value != 0 && index > 1) {
            buf[--index] = digits[value % base];
            value /= base;
        }
        char[] str = Arrays.copyOfRange(buf, index, buf.length);
        builder.append(str);
    }

    private static Val formatDecimal(StringBuilder builder, Val arg) {
        builder.append(arg.value());
        return NullT.NullValue;
    }

    private static Val formatOctal(StringBuilder builder, Val arg) {
        throw RuntimeError.newRuntimeErrorf("unimplemented formatOctal");
    }

    private static Val formatBinary(StringBuilder builder, Val arg) {
        throw RuntimeError.newRuntimeErrorf("unimplemented formatBinary");
    }

    private static Val formatFloating(StringBuilder builder, Val arg, int precision) {
        throw RuntimeError.newRuntimeErrorf("unimplemented formatFloating");
    }

    private static Val formatExponent(StringBuilder builder, Val arg, int precision) {
        throw RuntimeError.newRuntimeErrorf("unimplemented formatExponent");
    }
}
