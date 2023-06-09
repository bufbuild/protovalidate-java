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


import org.projectnessie.cel.interpreter.Activation;
import org.projectnessie.cel.interpreter.ResolvedValue;

import static org.projectnessie.cel.interpreter.ResolvedValue.ABSENT;

/**
 * Variable implements interpreter.Activation, providing a lightweight named
 * variable to cel.Program executions.
 */
public class Variable implements Activation {
    private Activation next;
    private String name;
    private Object val;

    public Variable(String name, Object val) {
        this.name = name;
        this.val = val;
    }

    public Variable() {
    }

    @Override
    public ResolvedValue resolveName(String name) {
        if (this.name.equals(name)) {
            return ResolvedValue.resolvedValue(val);
        } else if (next != null) {
            return next.resolveName(name);
        }
        return ABSENT;
    }

    @Override
    public Activation parent() {
        return null;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setObject(Object o) {
        this.val = o;
    }
}
