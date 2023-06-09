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

import build.buf.protovalidate.errors.ValidationError;
import build.buf.validate.Violation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

// ProgramSet is a list of compiledProgram expressions that are evaluated
// together with the same input value. All expressions in a ProgramSet may refer
// to a `this` variable.
public class ProgramSet {
    public final List<CompiledProgram> programs;

    public ProgramSet(List<CompiledProgram> programs) {
        this.programs = programs;
    }

    public void set(int index, CompiledProgram program) {
        this.programs.set(index, program);
    }

    public ValidationError eval(Object val, boolean failFast) {
        Variable variable = new Variable();
        variable.setName("this");
//        if (val instanceof Message) {
//            variable.setObject(((Message) val).getDefaultInstanceForType());
//        } else if (val instanceof MapEntry) {
//            // TODO: com.google.protobuf.MapEntry is not the right type
//        } else {
//
//        }
        variable.setObject(val);
        List<Violation> violations = new ArrayList<>();
        for (CompiledProgram program : programs) {
            Violation violation = program.eval(variable);
            if (violation != null) {
                violations.add(violation);
                if (failFast) {
                    break;
                }
            }
        }
        if (!violations.isEmpty()) {
            return new ValidationError(violations);
        }
        return null;
}

}
