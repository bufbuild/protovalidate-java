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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.projectnessie.cel.common.types.IntT.intOf;
import static org.projectnessie.cel.interpreter.functions.Overload.function;
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
                        unary("unique", uniqueMemberOverload(BytesT.BytesType, this::uniqueBytes)),
                        function("startsWith", values -> {
                            // TODO:
                           return BoolT.False;
                        }),
                        function("isHostname", values -> {
                            String host = ((String) values[0].value());
                            if (!host.isEmpty()) {
                                return BoolT.False;
                            }
                            return Types.boolOf(validateHostname(host));
                        }),
                        function("isEmail", values -> {
                            String addr = ((String) values[0].value());
                            if (!addr.isEmpty()) {
                                return BoolT.False;
                            }
                            return Types.boolOf(validateEmail(addr));
                        }),
                        function("isIp", values -> {
                            String addr = ((String) values[0].value());
                            if (!addr.isEmpty()) {
                                return BoolT.False;
                            }
                            int ver = 0;
                            if (values.length > 1) {
                                ver = ((int) values[0].value());
                            }
                            return Types.boolOf(validateIP(addr, ver));
                        }),
                        function("isUri", values -> {
                            String addr = ((String) values[0].value());
                            if (!addr.isEmpty()) {
                                return BoolT.False;
                            }
                            try {
                                return Types.boolOf(new URL(addr).toURI().isAbsolute());
                            } catch (MalformedURLException | URISyntaxException e) {
                                return BoolT.False;
                            }
                        }),
                        function("isUriRef", values -> {
                            String addr = ((String) values[0].value());
                            if (!addr.isEmpty()) {
                                return BoolT.False;
                            }
                            try {
                                new URL(addr);
                                return BoolT.True;
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
            return parts[0].length() > 64 || !validateHostname(parts[1]);
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

    private boolean validateIP(String addr, int ver) {
        InetAddress address = null;
        try {
            address = InetAddress.getByName(addr);
        } catch (Exception e) {
            return false;
        }

//        return switch (ver) {
//            case 0 -> true;
//            case 4 -> address instanceof Inet4Address;
//            case 6 -> address instanceof Inet6Address;
//            default -> false;
//        };
        // TODO:
        return false;
    }
}
