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

import build.buf.protovalidate.expression.NowVariable;
import com.google.api.expr.v1alpha1.Decl;
import org.projectnessie.cel.EnvOption;
import org.projectnessie.cel.EvalOption;
import org.projectnessie.cel.Library;
import org.projectnessie.cel.ProgramOption;
import org.projectnessie.cel.checker.Decls;
import org.projectnessie.cel.common.types.*;
import org.projectnessie.cel.common.types.ref.Val;
import org.projectnessie.cel.common.types.traits.Lister;
import org.projectnessie.cel.interpreter.functions.UnaryOp;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.projectnessie.cel.common.types.IntT.intOf;
import static org.projectnessie.cel.interpreter.functions.Overload.binary;
import static org.projectnessie.cel.interpreter.functions.Overload.function;
import static org.projectnessie.cel.interpreter.functions.Overload.overload;
import static org.projectnessie.cel.interpreter.functions.Overload.unary;

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
        List<Decl.FunctionDecl.Overload> formatOverloads = new ArrayList<>();
        // TODO: Iterate exhaustively
        for (com.google.api.expr.v1alpha1.Type type : Arrays.asList(Decls.String, Decls.Int, Decls.Uint, Decls.Double, Decls.Bytes)) {
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
        }
        Decl formatFunction = Decls.newFunction("format",
               formatOverloads
        );
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
                                                "starts_with_byts",
                                                Arrays.asList(Decls.Bytes, Decls.Bytes),
                                                Decls.Bool
                                        )
                                ),
                                formatFunction
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
                            String format = String.format(Locale.US, lhs.toString(), (Object[]) rhs.value());
                            return StringT.stringOf(format);
                        }),
                        unary("unique", uniqueMemberOverload(BytesT.BytesType, this::uniqueBytes)),
                        binary("startsWith", (lhs, rhs) -> {
                            String str = lhs.value().toString();
                            return str.startsWith(rhs.value().toString()) ? BoolT.True : BoolT.False;
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

    public UnaryOp uniqueMemberOverload(org.projectnessie.cel.common.types.ref.Type itemType, overloadFunc overload) {
        return value -> {
            Lister list = (Lister) value;
            if (list == null) {
                return Err.unsupportedRefValConversionErr(value);
            }
            if (list.type() != itemType.type()) {
                return Err.newTypeConversionError(list.type(), itemType.type());
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
            address = InetAddress.getByName(addr);
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
