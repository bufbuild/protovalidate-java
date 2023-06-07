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

package build.buf.protovalidate.evaluator;

import build.buf.protovalidate.errors.ValidationError;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;

import java.util.Set;

public class Any implements Evaluator {
    private Descriptors.FieldDescriptor typeURLDescriptor;
    private Set<String> in;
    private Set<String> notIn;


    public Any(Descriptors.FieldDescriptor typeURLDescriptor, Set<String> in, Set<String> notIn) {
        this.typeURLDescriptor = typeURLDescriptor;
        this.in = in;
        this.notIn = notIn;
    }

    @Override
    public void evaluate(DynamicMessage val, boolean failFast) throws ValidationError {}

    public boolean tautology() {
        return in.size() == 0 && notIn.size() == 0;
    }

    public static Set<String> stringsToSet(String[] ss) {
        return null;
    }
}