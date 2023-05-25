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

import build.buf.validate.Constraint;
import build.buf.validate.FieldConstraints;
import build.buf.validate.MessageConstraints;
import build.buf.validate.ValidateProto;
import com.google.protobuf.*;

import java.util.List;

public class Validate {

    public void validate(Message message) {
        MessageConstraints f = message.getDescriptorForType().getOptions().getExtension(ValidateProto.message);
        if (message.getDescriptorForType().isExtendable()) {
            // Need a loop for all present fields. Check for extensions in this loop to
            // validate extensions. The non-extension fields are checked in the loop below.
            return;
        }
        for (Descriptors.FieldDescriptor field: message.getDescriptorForType().getFields()) {
            if (field.getOptions().hasExtension(ValidateProto.field)) {
                FieldConstraints extension = field.getOptions().getExtension(ValidateProto.field);
                List<Constraint> celList = extension.getCelList();
            }
        }
    }
}
