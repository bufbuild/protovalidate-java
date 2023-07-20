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

import com.google.api.expr.v1alpha1.Decl;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.projectnessie.cel.checker.Decls;
import org.projectnessie.cel.common.types.TimestampT;

/** Defines custom declaration functions. */
final class CustomDeclarations {

  /**
   * Create the custom function declaration list.
   *
   * @return the list of function declarations.
   */
  static List<Decl> create() {
    List<Decl> decls = new ArrayList<>();

    // Add 'now' variable declaration
    decls.add(Decls.newVar("now", Decls.newObjectType(TimestampT.TimestampType.typeName())));

    // Add 'isIp' function declaration
    decls.add(
        Decls.newFunction(
            "isIp",
            Decls.newInstanceOverload("is_ip", Arrays.asList(Decls.String, Decls.Int), Decls.Bool),
            Decls.newInstanceOverload(
                "is_ip_unary", Collections.singletonList(Decls.String), Decls.Bool)));

    // Add 'isUriRef' function declaration
    decls.add(
        Decls.newFunction(
            "isUriRef",
            Decls.newInstanceOverload(
                "is_uri_ref", Collections.singletonList(Decls.String), Decls.Bool)));

    // Add 'isUri' function declaration
    decls.add(
        Decls.newFunction(
            "isUri",
            Decls.newInstanceOverload(
                "is_uri", Collections.singletonList(Decls.String), Decls.Bool)));

    // Add 'isEmail' function declaration
    decls.add(
        Decls.newFunction(
            "isEmail",
            Decls.newInstanceOverload(
                "is_email", Collections.singletonList(Decls.String), Decls.Bool)));

    // Add 'isHostname' function declaration
    decls.add(
        Decls.newFunction(
            "isHostname",
            Decls.newInstanceOverload(
                "is_hostname", Collections.singletonList(Decls.String), Decls.Bool)));

    // Add 'startsWith' function declaration
    decls.add(
        Decls.newFunction(
            "startsWith",
            Decls.newInstanceOverload(
                "starts_with_bytes", Arrays.asList(Decls.Bytes, Decls.Bytes), Decls.Bool)));

    // Add 'endsWith' function declaration
    decls.add(
        Decls.newFunction(
            "endsWith",
            Decls.newInstanceOverload(
                "ends_with_bytes", Arrays.asList(Decls.Bytes, Decls.Bytes), Decls.Bool)));

    // Add 'contains' function declaration
    decls.add(
        Decls.newFunction(
            "contains",
            Decls.newInstanceOverload(
                "contains_bytes", Arrays.asList(Decls.Bytes, Decls.Bytes), Decls.Bool)));

    // Add 'unique' function declaration
    List<Decl.FunctionDecl.Overload> uniqueOverloads = new ArrayList<>();
    for (com.google.api.expr.v1alpha1.Type type :
        Arrays.asList(Decls.String, Decls.Int, Decls.Uint, Decls.Double, Decls.Bytes, Decls.Bool)) {
      uniqueOverloads.add(
          Decls.newInstanceOverload(
              String.format(
                  "unique_%s",
                  org.projectnessie.cel.checker.Types.formatCheckedType(type)
                      .toLowerCase(Locale.US)),
              Collections.singletonList(type),
              Decls.Bool));
      uniqueOverloads.add(
          Decls.newInstanceOverload(
              String.format(
                  "unique_list_%s",
                  org.projectnessie.cel.checker.Types.formatCheckedType(type)
                      .toLowerCase(Locale.US)),
              Collections.singletonList(Decls.newListType(type)),
              Decls.Bool));
    }
    decls.add(Decls.newFunction("unique", uniqueOverloads));

    // Add 'format' function declaration
    List<Decl.FunctionDecl.Overload> formatOverloads = new ArrayList<>();
    for (com.google.api.expr.v1alpha1.Type type :
        Arrays.asList(
            Decls.String,
            Decls.Int,
            Decls.Uint,
            Decls.Double,
            Decls.Bytes,
            Decls.Bool,
            Decls.Duration,
            Decls.Timestamp)) {
      formatOverloads.add(
          Decls.newInstanceOverload(
              String.format(
                  "format_%s",
                  org.projectnessie.cel.checker.Types.formatCheckedType(type)
                      .toLowerCase(Locale.US)),
              Arrays.asList(Decls.String, Decls.newListType(type)),
              Decls.String));
      formatOverloads.add(
          Decls.newInstanceOverload(
              String.format(
                  "format_list_%s",
                  org.projectnessie.cel.checker.Types.formatCheckedType(type)
                      .toLowerCase(Locale.US)),
              Arrays.asList(Decls.String, Decls.newListType(Decls.newListType(type))),
              Decls.String));
      formatOverloads.add(
          Decls.newInstanceOverload(
              String.format(
                  "format_bytes_%s",
                  org.projectnessie.cel.checker.Types.formatCheckedType(type)
                      .toLowerCase(Locale.US)),
              Arrays.asList(Decls.Bytes, Decls.newListType(type)),
              Decls.Bytes));
      formatOverloads.add(
          Decls.newInstanceOverload(
              String.format(
                  "format_bytes_list_%s",
                  org.projectnessie.cel.checker.Types.formatCheckedType(type)
                      .toLowerCase(Locale.US)),
              Arrays.asList(Decls.Bytes, Decls.newListType(Decls.newListType(type))),
              Decls.Bytes));
    }
    decls.add(Decls.newFunction("format", formatOverloads));

    return Collections.unmodifiableList(decls);
  }
}
