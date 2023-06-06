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

package build.buf.protovalidatejava.build.buf;

import build.buf.protovalidate.Errors.ValidationError;
import build.buf.protovalidate.Validator;
import build.buf.validate.conformance.cases.StringConst;
import build.buf.validate.java.Simple;
import org.junit.Test;
import org.projectnessie.cel.tools.ScriptCreateException;
import org.projectnessie.cel.tools.ScriptException;

public class ValidationTest {

    @Test
    public void test() {
        Validator validator = new Validator(new Validator.Config());
        validator.validate(Simple.newBuilder().setA(100).build());
    }
}
