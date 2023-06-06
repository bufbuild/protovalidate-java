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

package build.buf.protovalidate.Expression;

import build.buf.protovalidate.Errors.ValidationError;

import java.util.*;

// ProgramSet is a list of compiledProgram expressions that are evaluated
// together with the same input value. All expressions in a ProgramSet may refer
// to a `this` variable.
public class ProgramSet {
    private List<CompiledProgram> programs;

    public ProgramSet(int size) {
        this.programs = new ArrayList<>(Collections.nCopies(size, null));
    }

    public void set(int index, CompiledProgram program) {
        this.programs.set(index, program);
    }

    public ValidationError eval(Object val, boolean failFast) throws Exception {
        return null;
    }

}