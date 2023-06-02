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

package build.buf.protovalidate.Constraints;

import build.buf.protovalidate.Expression.AstSet;
import build.buf.protovalidate.Expression.ProgramSet;
import build.buf.validate.FieldConstraints;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import dev.cel.common.types.CelType;
import dev.cel.runtime.CelRuntime;

import java.util.HashMap;
import java.util.Map;

public class Cache {
    private final Map<FieldDescriptor, AstSet> cache;

    public Cache() {
        this.cache = new HashMap<>();
    }

    public Message resolveConstraints(FieldDescriptor fieldDesc, FieldConstraints fieldConstraints, Boolean forItems) {
        // TODO: implement me
        return null;
    }

    public CelRuntime prepareEnvironment(CelRuntime env, FieldDescriptor fieldDesc, Message rules, Boolean forItems) {
        // TODO: implement me
        return null;
    }

    public AstSet loadOrCompileStandardConstraint(CelRuntime env, FieldDescriptor constraintFieldDesc) {
        // TODO: implement me
        return null;
    }

    public FieldDescriptor getExpectedConstraintDescriptor(FieldDescriptor targetFieldDesc, Boolean forItems) {
        // TODO: implement me
        return null;
    }

    public CelType getCELType(FieldDescriptor fieldDesc, Boolean forItems) {
        // TODO: implement me
        return null;
    }

    public ProgramSet Build(CelRuntime env, FieldDescriptor fieldDesc, FieldConstraints fieldConstraints, Boolean forItems) {
        // TODO: implement me
        return null;
    }
}