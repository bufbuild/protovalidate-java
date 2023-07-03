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

package build.buf.protovalidate;

import build.buf.protovalidate.evaluator.ConstraintResolver;
import com.google.protobuf.Descriptors;

import java.util.Collections;
import java.util.List;

public class Config {
    public final boolean failFast;
    // TODO: does nothing
    public final boolean useUTC;
    public final boolean disableLazy;
    public final ConstraintResolver resolver;

    public Config(boolean failFast, boolean useUTC, boolean disableLazy, ConstraintResolver resolver) {
        this.failFast = failFast;
        this.useUTC = useUTC;
        this.disableLazy = disableLazy;
        this.resolver = resolver;
    }

    public Config() {
        this(false, true, false, new ConstraintResolver());
    }
}
