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

package build.buf.protovalidate.expression;

// Compile produces a ProgramSet from the provided expressions in the given
// environment. If the generated cel.Program require cel.ProgramOption params,
// use CompileASTs instead with a subsequent call to AstSet.ToProgramSet.
public class Compiler  {
//    public static <T extends Constraint> ProgramSet compile(List<T> expressions, CelRuntimeBuilder env, CelOptions... envOpts) throws Exception {
//        return null;
//    }
//
//    // CompileASTs parses and type checks a set of expressions, producing a resulting
//    // AstSet. The value can then be converted to a ProgramSet via
//    // AstSet.ToProgramSet or AstSet.ReduceResiduals. Use Compile instead if no
//    // cel.ProgramOption args need to be provided or residuals do not need to be
//    // computed.
//    public static <T extends Constraint> AstSet compileASTs(List<T> expressions, CelRuntimeBuilder env, CelOptions... envOpts) throws Exception {
//        return null;
//    }
//
//    private static CompiledAst compileAST(CelRuntimeBuilder env, Constraint expr) throws Exception {
//        return null;
//    }
}