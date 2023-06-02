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

package build.buf.protovalidate.Evaluator;

import build.buf.validate.FieldConstraints;
import build.buf.validate.MessageConstraints;
import build.buf.validate.OneofConstraints;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Extension;
import com.google.protobuf.GeneratedMessageV3.ExtendableMessage;
import com.google.protobuf.Message;

public class DefaultResolver implements StandardConstraintResolver {

    @Override
    public MessageConstraints resolveMessageConstraints(Descriptors.Descriptor desc) {
        return null;
    }

    @Override
    public OneofConstraints resolveOneofConstraints(Descriptors.OneofDescriptor desc) {
        return null;
    }

    @Override
    public FieldConstraints resolveFieldConstraints(Descriptors.FieldDescriptor desc) {
        return null;
    }

    private <D extends ExtendableMessage, C extends Message> C resolveExtension(D desc, Extension<D, C> extType) {
        return null;
    }
}
