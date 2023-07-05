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
import org.projectnessie.cel.common.types.ref.Type;
import org.projectnessie.cel.common.types.ref.Val;

import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.List;

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
            return formatStringSafe(builder, val, false);
        }
    }

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
            builder.append("\"")
                    .append(val.value().toString())
                    .append("\"");
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

    private static void formatList(StringBuilder builder, Val val) {
        builder.append('[');
        List list = val.convertToNative(List.class);
        for (int i = 0; i < list.size(); i++) {
            Object obj = list.get(i);
            formatStringSafe(builder, nativeToValue(Db.newDb(), null, obj), true);
            if (i != list.size() - 1) {
                builder.append(", ");
            }
        }
        builder.append(']');
    }

    private static void formatTimestamp(StringBuilder builder, Val val) {
        builder.append("timestamp(");
        Timestamp timestamp = val.convertToNative(Timestamp.class);
        builder.append(timestamp.toString());
        builder.append(")");
    }

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

    private static void formatBytes(StringBuilder builder, Val val) {
        builder.append("\"")
                .append(new String((byte[]) val.value(), StandardCharsets.UTF_8))
                .append("\"");
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
            throw new RuntimeException("formatHex: expected int or string");
        }
        builder.append(hexString);
        return NullT.NullType;
    }

    private static Val formatDecimal(StringBuilder builder, Val arg) {
        builder.append(arg.value());
        return NullT.NullValue;
    }

    private static Val formatOctal(StringBuilder builder, Val arg) {
        throw new RuntimeException("unimplemented formatOctal");
    }

    private static Val formatBinary(StringBuilder builder, Val arg) {
        throw new RuntimeException("unimplemented formatBinary");
    }

    private static Val formatFloating(StringBuilder builder, Val arg, int precision) {
        throw new RuntimeException("unimplemented formatFloating");
    }

    private static Val formatExponent(StringBuilder builder, Val arg, int precision) {
        throw new RuntimeException("unimplemented formatExponent");
    }
}
