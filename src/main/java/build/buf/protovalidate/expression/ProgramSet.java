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
import build.buf.protovalidate.evaluator.JavaValue;
import build.buf.validate.Violation;
import com.google.protobuf.Message;
import org.projectnessie.cel.interpreter.ResolvedValue;

import java.util.ArrayList;
import java.util.Collections;
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

    public ValidationError eval(Object val, boolean failFast) {
//        if (val instanceof Message) {
//            variable.setObject(((Message) val).getDefaultInstanceForType());
//        } else if (val instanceof MapEntry) {
//            // TODO: com.google.protobuf.MapEntry is not the right type
//        } else {
//
//        }
        Object value;
        if (val instanceof JavaValue) {
            value = ((JavaValue) val).value();
        } else if (val instanceof Message) {
            value = val;
        } else {
            throw new RuntimeException("unsupported type for " + val.getClass());
        }
        // todo: weird api
        Variable activation = new Variable(new NowVariable(), "this", value);
        List<Violation> violations = new ArrayList<>();
        for (CompiledProgram program : programs) {
            Violation violation = program.eval(activation);
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

    public boolean isEmpty() {
        return programs.isEmpty();
    }
}