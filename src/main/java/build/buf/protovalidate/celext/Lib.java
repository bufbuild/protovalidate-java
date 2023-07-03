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

import build.buf.protovalidate.results.ExecutionException;
import build.buf.protovalidate.expression.NowVariable;
import com.google.api.expr.v1alpha1.Decl;
import com.google.common.net.InetAddresses;
import com.google.common.primitives.Bytes;
import org.projectnessie.cel.EnvOption;
import org.projectnessie.cel.EvalOption;
import org.projectnessie.cel.Library;
import org.projectnessie.cel.ProgramOption;
import org.projectnessie.cel.checker.Decls;
import org.projectnessie.cel.common.types.BoolT;
import org.projectnessie.cel.common.types.BytesT;
import org.projectnessie.cel.common.types.DoubleT;
import org.projectnessie.cel.common.types.Err;
import org.projectnessie.cel.common.types.IntT;
import org.projectnessie.cel.common.types.ListT;
import org.projectnessie.cel.common.types.StringT;
import org.projectnessie.cel.common.types.TimestampT;
import org.projectnessie.cel.common.types.Types;
import org.projectnessie.cel.common.types.UintT;
import org.projectnessie.cel.common.types.ref.Type;
import org.projectnessie.cel.common.types.ref.Val;
import org.projectnessie.cel.common.types.traits.Lister;
import org.projectnessie.cel.interpreter.functions.UnaryOp;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.projectnessie.cel.common.types.IntT.intOf;
import static org.projectnessie.cel.interpreter.functions.Overload.*;

public class Lib implements Library {
    private boolean useUtc;

    public Lib(boolean useUtc) {
        // TODO: Implement me
        this.useUtc = useUtc;
    }

    @Override
    public List<EnvOption> getCompileOptions() {
        List<EnvOption> opts = new ArrayList<>();
        opts.add(EnvOption.declarations(
                Decls.newVar("now", Decls.newObjectType(TimestampT.TimestampType.typeName()))
        ));
        List<Decl.FunctionDecl.Overload> uniqueOverloads = new ArrayList<>();
        for (com.google.api.expr.v1alpha1.Type type : Arrays.asList(Decls.String, Decls.Int, Decls.Uint, Decls.Double, Decls.Bytes, Decls.Bool)) {
            uniqueOverloads.add(Decls.newInstanceOverload(
                    String.format("unique_%s", org.projectnessie.cel.checker.Types.formatCheckedType(type).toLowerCase(Locale.US)),
                    Collections.singletonList(type),
                    Decls.Bool
            ));
            uniqueOverloads.add(Decls.newInstanceOverload(
                    String.format("unique_list_%s", org.projectnessie.cel.checker.Types.formatCheckedType(type).toLowerCase(Locale.US)),
                    Collections.singletonList(Decls.newListType(type)),
                    Decls.Bool
            ));
        }
        List<Decl.FunctionDecl.Overload> formatOverloads = new ArrayList<>();
        for (com.google.api.expr.v1alpha1.Type type : Arrays.asList(Decls.String, Decls.Int, Decls.Uint, Decls.Double, Decls.Bytes, Decls.Bool, Decls.Duration, Decls.Timestamp)) {
            formatOverloads.add(Decls.newInstanceOverload(
                    String.format("format_%s", org.projectnessie.cel.checker.Types.formatCheckedType(type).toLowerCase(Locale.US)),
                    Arrays.asList(Decls.String, Decls.newListType(type)),
                    Decls.String
            ));
            formatOverloads.add(Decls.newInstanceOverload(
                    String.format("format_list_%s", org.projectnessie.cel.checker.Types.formatCheckedType(type).toLowerCase(Locale.US)),
                    Arrays.asList(Decls.String, Decls.newListType(Decls.newListType(type))),
                    Decls.String
            ));
            formatOverloads.add(Decls.newInstanceOverload(
                    String.format("format_bytes_%s", org.projectnessie.cel.checker.Types.formatCheckedType(type).toLowerCase(Locale.US)),
                    Arrays.asList(Decls.Bytes, Decls.newListType(type)),
                    Decls.Bytes
            ));
            formatOverloads.add(Decls.newInstanceOverload(
                    String.format("format_bytes_list_%s", org.projectnessie.cel.checker.Types.formatCheckedType(type).toLowerCase(Locale.US)),
                    Arrays.asList(Decls.Bytes, Decls.newListType(Decls.newListType(type))),
                    Decls.Bytes
            ));
        }
        opts.addAll(
                Arrays.asList(
                        EnvOption.declarations(
                                Decls.newFunction(
                                        "isIp",
                                        Decls.newInstanceOverload(
                                                "is_ip",
                                                Arrays.asList(Decls.String, Decls.Int),
                                                Decls.Bool
                                        ),
                                        Decls.newInstanceOverload(
                                                "is_ip_unary",
                                                Collections.singletonList(Decls.String),
                                                Decls.Bool
                                        )
                                ),
                                Decls.newFunction("isUriRef",
                                        Decls.newInstanceOverload(
                                                "is_uri_ref",
                                                Collections.singletonList(Decls.String),
                                                Decls.Bool
                                        )
                                ),
                                Decls.newFunction("isUri",
                                        Decls.newInstanceOverload(
                                                "is_uri",
                                                Collections.singletonList(Decls.String),
                                                Decls.Bool
                                        )
                                ),
                                Decls.newFunction("isEmail",
                                        Decls.newInstanceOverload(
                                                "is_email",
                                                Collections.singletonList(Decls.String),
                                                Decls.Bool
                                        )
                                ),
                                Decls.newFunction("isHostname",
                                        Decls.newInstanceOverload(
                                                "is_hostname",
                                                Collections.singletonList(Decls.String),
                                                Decls.Bool
                                        )
                                ),
                                Decls.newFunction("startsWith",
                                        Decls.newInstanceOverload(
                                                "starts_with_bytes",
                                                Arrays.asList(Decls.Bytes, Decls.Bytes),
                                                Decls.Bool
                                        )
                                ),
                                Decls.newFunction("endsWith",
                                        Decls.newInstanceOverload(
                                                "ends_with_bytes",
                                                Arrays.asList(Decls.Bytes, Decls.Bytes),
                                                Decls.Bool
                                        )
                                ),
                                Decls.newFunction("contains",
                                        Decls.newInstanceOverload(
                                                "contains_bytes",
                                                Arrays.asList(Decls.Bytes, Decls.Bytes),
                                                Decls.Bool
                                        )
                                ),
                                Decls.newFunction("unique",
                                        uniqueOverloads
                                ),
                                Decls.newFunction("format",
                                        formatOverloads
                                )
                        )
                )
        );
        return opts;
    }

    @Override
    public List<ProgramOption> getProgramOptions() {
        List<ProgramOption> opts = new ArrayList<>();
        opts.add(ProgramOption.evalOptions(
                EvalOption.OptOptimize
        ));
        opts.add(ProgramOption.globals(new NowVariable()));
        ProgramOption functions =
                ProgramOption.functions(
                        binary("format", (lhs, rhs) -> {
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
                        }),
                        unary("unique", (val) -> {
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
                        }),
                        binary("startsWith", (lhs, rhs) -> {
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
                        }),
                        binary("endsWith", (lhs, rhs) -> {
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
                        }),
                        binary("contains", (lhs, rhs) -> {
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
                        }),
                        unary("isHostname", value -> {
                            String host = value.value().toString();
                            if (host.isEmpty()) {
                                return BoolT.False;
                            }
                            return Types.boolOf(validateHostname(host));
                        }),
                        unary("isEmail", value -> {
                            String addr = value.value().toString();
                            if (addr.isEmpty()) {
                                return BoolT.False;
                            }
                            return Types.boolOf(validateEmail(addr));
                        }),
                        overload(
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
                                null
                        ),
                        unary("isUri", value -> {
                            String addr = value.value().toString();
                            if (addr.isEmpty()) {
                                return BoolT.False;
                            }
                            try {
                                return Types.boolOf(new URL(addr).toURI().isAbsolute());
                            } catch (MalformedURLException | URISyntaxException e) {
                                return BoolT.False;
                            }
                        }),
                        unary("isUriRef", value -> {
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
                        })
                );
        opts.add(functions);
        return opts;
    }

    private UnaryOp unaryOpForPrimitiveVal(Val val) {
        switch (val.type().typeEnum()) {
            case Bool:
                return uniqueMemberOverload(BoolT.BoolType, this::uniqueScalar);
            case Bytes:
                return uniqueMemberOverload(BytesT.BytesType, this::uniqueBytes);
            case Double:
                return uniqueMemberOverload(DoubleT.DoubleType, this::uniqueScalar);
            case Int:
                return uniqueMemberOverload(IntT.IntType, this::uniqueScalar);
            case String:
                return uniqueMemberOverload(StringT.StringType, this::uniqueScalar);
            case Uint:
                return uniqueMemberOverload(UintT.UintType, this::uniqueScalar);
            default:
                return Err::maybeNoSuchOverloadErr;
        }
    }

    public UnaryOp uniqueMemberOverload(org.projectnessie.cel.common.types.ref.Type itemType, overloadFunc overload) {
        return value -> {
            Lister list = (Lister) value;
            if (list == null || list.size().intValue() == 0L) {
                // TODO: find appropriate return error
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
     * uniqueBytes is an overload implementation of the unique function that
     * compares bytes type CEL values. This function is used instead of uniqueScalar
     * as the bytes ([]uint8) type is not hashable in Go; we cheat this by converting
     * the value to a string.
     *
     * @param list which aggregates the traits of a list.
     * @return Val interface defines the functions supported by all expression values.
     * Val's implementations may specialize the behavior of the value through the
     * addition of traits.
     */
    private Val uniqueBytes(Lister list) {
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

    public Val uniqueScalar(Lister list) {
        // TODO: dont like the use of map here but it works
        Map<Val, Boolean> exist = new HashMap<>();
        for (int i = 0; i < list.size().intValue(); i++) {
            Val val = list.get(intOf(i));
            if (exist.containsKey(val)) {
                return BoolT.False;
            }
            exist.put(val, Boolean.TRUE);
        }
        return BoolT.True;
    }

    private boolean validateEmail(String addr) {
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

    private boolean validateHostname(String host) {
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

    private boolean validateIP(String addr, long ver) {
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
