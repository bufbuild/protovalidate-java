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

import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.projectnessie.cel.EnvOption.declarations;
import static org.projectnessie.cel.ProgramOption.functions;

public class ValidateLibrary implements Library {

    @Override
    public List<EnvOption> getCompileOptions() {
        return Collections.singletonList(declarations(CustomDecl.make()));
    }

    @Override
    public List<ProgramOption> getProgramOptions() {
        return asList(
                ProgramOption.evalOptions(EvalOption.OptOptimize),
                ProgramOption.globals(new NowVariable()),
                functions(CustomOverload.make())
        );
    }
}
