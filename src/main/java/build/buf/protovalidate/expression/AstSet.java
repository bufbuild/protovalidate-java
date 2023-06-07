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

import org.projectnessie.cel.Env;
import org.projectnessie.cel.ProgramOption;

import java.util.List;

// AstSet represents a collection of CompiledAst and their associated CelRuntime.
public class AstSet {
    private List<CompiledAst> asts;
    private final Env env;

    public AstSet(Env env, List<CompiledAst> asts) {
        this.env = env;
        this.asts = asts;
    }

    // Merge combines a set with another, producing a new AstSet.
    public AstSet merge(AstSet other) {
        return null;
    }

    // ReduceResiduals generates a ProgramSet, performing a partial evaluation of
    // the AstSet to optimize the expression. If the expression is optimized to
    // either a true or empty string constant result, no CompiledProgram is
    // generated for it. The main usage of this is to elide tautological expressions
    // from the final result.
    public ProgramSet reduceResiduals(ProgramOption... opts) throws Exception {
        return null;
    }

    // ToProgramSet generates a ProgramSet from the specified ASTs.
    public ProgramSet toProgramSet(ProgramOption... opts) throws Exception {
       return null;
    }
}