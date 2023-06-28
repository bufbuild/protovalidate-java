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

import build.buf.validate.FieldConstraints;
import build.buf.validate.MessageConstraints;
import build.buf.validate.OneofConstraints;
import build.buf.validate.ValidateProto;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.OneofDescriptor;

// TODO: potential for non registered classes to be read here maybe:
// https://github.com/bufbuild/protovalidate-go/blob/main/internal/evaluator/resolver.go#L43-L47
public class ConstraintResolver {

    public MessageConstraints resolveMessageConstraints(Descriptor desc) {
        DescriptorProtos.MessageOptions options = desc.getOptions();
        if (!options.hasExtension(ValidateProto.message)) {
            return MessageConstraints.newBuilder()
                    .build();
        }

        MessageConstraints constraints = options.getExtension(ValidateProto.message);
        boolean disabled = constraints.getDisabled();
        if (disabled) {
            return MessageConstraints.newBuilder()
                    .setDisabled(true)
                    .build();
        }
        return constraints;
    }

    public OneofConstraints resolveOneofConstraints(OneofDescriptor desc) {
        DescriptorProtos.OneofOptions options = desc.getOptions();
        if (!options.hasExtension(ValidateProto.oneof)) {
            return OneofConstraints.newBuilder()
                    .build();
        }
        return options.getExtension(ValidateProto.oneof);
    }

    public FieldConstraints resolveFieldConstraints(FieldDescriptor desc) {
        DescriptorProtos.FieldOptions options = desc.getOptions();
        if (!options.hasExtension(ValidateProto.field)) {
            return FieldConstraints.newBuilder().build();
        }
        return options.getExtension(ValidateProto.field);
    }
}
