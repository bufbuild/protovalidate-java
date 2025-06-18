// Copyright 2023-2025 Buf Technologies, Inc.
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

import static dev.cel.common.CelFunctionDecl.newFunctionDeclaration;
import static dev.cel.common.CelOverloadDecl.newGlobalOverload;
import static dev.cel.common.CelOverloadDecl.newMemberOverload;

import dev.cel.common.CelFunctionDecl;
import dev.cel.common.CelOverloadDecl;
import dev.cel.common.types.CelType;
import dev.cel.common.types.ListType;
import dev.cel.common.types.SimpleType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Defines custom declaration functions. */
final class CustomDeclarations {

  /**
   * Create the custom function declaration list.
   *
   * @return the list of function declarations.
   */
  static List<CelFunctionDecl> create() {
    List<CelFunctionDecl> decls = new ArrayList<>();

    // Add 'getField' function declaration
    decls.add(
        newFunctionDeclaration(
            "getField",
            newGlobalOverload(
                "get_field_any_string",
                SimpleType.DYN,
                Arrays.asList(SimpleType.ANY, SimpleType.STRING))));
    // Add 'isIp' function declaration
    decls.add(
        newFunctionDeclaration(
            "isIp",
            newMemberOverload(
                "is_ip", SimpleType.BOOL, Arrays.asList(SimpleType.STRING, SimpleType.INT)),
            newMemberOverload(
                "is_ip_unary", SimpleType.BOOL, Collections.singletonList(SimpleType.STRING))));

    // Add 'isIpPrefix' function declaration
    decls.add(
        newFunctionDeclaration(
            "isIpPrefix",
            newMemberOverload(
                "is_ip_prefix_int_bool",
                SimpleType.BOOL,
                Arrays.asList(SimpleType.STRING, SimpleType.INT, SimpleType.BOOL)),
            newMemberOverload(
                "is_ip_prefix_int",
                SimpleType.BOOL,
                Arrays.asList(SimpleType.STRING, SimpleType.INT)),
            newMemberOverload(
                "is_ip_prefix_bool",
                SimpleType.BOOL,
                Arrays.asList(SimpleType.STRING, SimpleType.BOOL)),
            newMemberOverload(
                "is_ip_prefix", SimpleType.BOOL, Collections.singletonList(SimpleType.STRING))));

    // Add 'isUriRef' function declaration
    decls.add(
        newFunctionDeclaration(
            "isUriRef",
            newMemberOverload(
                "is_uri_ref", SimpleType.BOOL, Collections.singletonList(SimpleType.STRING))));

    // Add 'isUri' function declaration
    decls.add(
        newFunctionDeclaration(
            "isUri",
            newMemberOverload(
                "is_uri", SimpleType.BOOL, Collections.singletonList(SimpleType.STRING))));

    // Add 'isEmail' function declaration
    decls.add(
        newFunctionDeclaration(
            "isEmail",
            newMemberOverload(
                "is_email", SimpleType.BOOL, Collections.singletonList(SimpleType.STRING))));

    // Add 'isHostname' function declaration
    decls.add(
        newFunctionDeclaration(
            "isHostname",
            newMemberOverload(
                "is_hostname", SimpleType.BOOL, Collections.singletonList(SimpleType.STRING))));

    decls.add(
        newFunctionDeclaration(
            "isHostAndPort",
            newMemberOverload(
                "string_bool_is_host_and_port_bool",
                SimpleType.BOOL,
                Arrays.asList(SimpleType.STRING, SimpleType.BOOL))));

    // Add 'startsWith' function declaration
    decls.add(
        newFunctionDeclaration(
            "startsWith",
            newMemberOverload(
                "starts_with_bytes",
                SimpleType.BOOL,
                Arrays.asList(SimpleType.BYTES, SimpleType.BYTES))));

    // Add 'endsWith' function declaration
    decls.add(
        newFunctionDeclaration(
            "endsWith",
            newMemberOverload(
                "ends_with_bytes",
                SimpleType.BOOL,
                Arrays.asList(SimpleType.BYTES, SimpleType.BYTES))));

    // Add 'contains' function declaration
    decls.add(
        newFunctionDeclaration(
            "contains",
            newMemberOverload(
                "contains_bytes",
                SimpleType.BOOL,
                Arrays.asList(SimpleType.BYTES, SimpleType.BYTES))));

    // Add 'isNan' function declaration
    decls.add(
        newFunctionDeclaration(
            "isNan",
            newMemberOverload(
                "is_nan", SimpleType.BOOL, Collections.singletonList(SimpleType.DOUBLE))));

    // Add 'isInf' function declaration
    decls.add(
        newFunctionDeclaration(
            "isInf",
            newMemberOverload(
                "is_inf_unary", SimpleType.BOOL, Collections.singletonList(SimpleType.DOUBLE)),
            newMemberOverload(
                "is_inf_binary",
                SimpleType.BOOL,
                Arrays.asList(SimpleType.DOUBLE, SimpleType.INT))));

    // Add 'unique' function declaration
    List<CelOverloadDecl> uniqueOverloads = new ArrayList<>();
    for (CelType type :
        Arrays.asList(
            SimpleType.STRING,
            SimpleType.INT,
            SimpleType.UINT,
            SimpleType.DOUBLE,
            SimpleType.BYTES,
            SimpleType.BOOL)) {
      uniqueOverloads.add(
          newMemberOverload(
              String.format("unique_list_%s", type.name().toLowerCase(Locale.US)),
              SimpleType.BOOL,
              Collections.singletonList(ListType.create(type))));
    }
    decls.add(newFunctionDeclaration("unique", uniqueOverloads));

    // Add 'format' function declaration
    List<CelOverloadDecl> formatOverloads = new ArrayList<>();
    formatOverloads.add(
        newMemberOverload(
            "format_list_dyn",
            SimpleType.STRING,
            Arrays.asList(SimpleType.STRING, ListType.create(SimpleType.DYN))));

    decls.add(newFunctionDeclaration("format", formatOverloads));

    return Collections.unmodifiableList(decls);
  }
}
