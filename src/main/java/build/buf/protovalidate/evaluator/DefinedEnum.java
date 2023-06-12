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

import build.buf.protovalidate.ValidationResult;
import build.buf.protovalidate.errors.ValidationError;
import build.buf.validate.Violation;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.ProtocolMessageEnum;

import java.util.Arrays;
import java.util.List;

public class DefinedEnum implements Evaluator {
    private final List<Descriptors.EnumValueDescriptor> valueDescriptors;

    public DefinedEnum(Descriptors.EnumValueDescriptor... valueDescriptors) {
        this.valueDescriptors = Arrays.asList(valueDescriptors);
    }

    public boolean tautology() {
        return false;
    }

    @Override
    public ValidationResult evaluate(DynamicMessage val, boolean failFast) {
        ProtocolMessageEnum enumValue = (ProtocolMessageEnum) val.getField(val.getDescriptorForType().findFieldByName("enum"));
        if (!isValueValid(enumValue)) {
            ValidationError err = new ValidationError();
            err.addViolation(Violation.newBuilder()
                    .setConstraintId("enum.defined_only")
                    .setMessage("value must be one of the defined enum values")
                    .build());
            return new ValidationResult(err);
        }
        return ValidationResult.success();
    }

    @Override
    public void append(Evaluator eval) {
        throw new UnsupportedOperationException("append not supported for DefinedEnum");
    }

    private boolean isValueValid(ProtocolMessageEnum value) {
        for (Descriptors.EnumValueDescriptor descriptor : valueDescriptors) {
            if (descriptor.getNumber() == value.getNumber()) {
                return true;
            }
        }
        return false;
    }

}
