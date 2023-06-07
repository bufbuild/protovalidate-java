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


import org.projectnessie.cel.Ast;
import org.projectnessie.cel.Env;
import org.projectnessie.cel.ProgramOption;

public class CompiledAst {
    private Ast ast;
    private Expression source;

    public CompiledAst(Ast ast, Expression source) {
        this.ast = ast;
        this.source = source;
    }

    public CompiledProgram toProgram(Env env, ProgramOption... opts) throws Exception {
        return null;
    }
}
