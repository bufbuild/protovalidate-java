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

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.Descriptors;
import org.projectnessie.cel.common.ULong;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class JavaValue {
    private final Descriptors.FieldDescriptor fieldDescriptor;
    private final Object value;

    public JavaValue(Descriptors.FieldDescriptor fieldDescriptor, Object value) {
        this.fieldDescriptor = fieldDescriptor;
        this.value = value;
    }

    public <T> T value() {
        Descriptors.FieldDescriptor.Type type = fieldDescriptor.getType();
        if (!fieldDescriptor.isRepeated() && (type == Descriptors.FieldDescriptor.Type.UINT32
                || type == Descriptors.FieldDescriptor.Type.UINT64
                || type == Descriptors.FieldDescriptor.Type.FIXED32
                || type == Descriptors.FieldDescriptor.Type.FIXED64)) {
            /* Java does not have native support for unsigned int/long or uint32/uint64 types.
            To work with CEL's uint type in Java, special handling is required.
            TL;DR: When using uint32/uint64 in your protobuf objects or CEL expressions in Java,
            wrap them with the org.projectnessie.cel.common.ULong type.*/
            return (T) ULong.valueOf(((Number) value).longValue());
        }
        // Dynamic programming in a static language.
        return (T) value;
    }

    public List<JavaValue> repeatedValue() {
        List<JavaValue> out = new ArrayList<>();
        if (fieldDescriptor.isRepeated()) {
            List<?> list = (List<?>) value;
            for (Object o : list) {
                out.add(new JavaValue(fieldDescriptor, o));
            }
        }
        return out;
    }

    public Map<JavaValue, JavaValue> mapValue() {
        Map<JavaValue, JavaValue> out = new HashMap<>();
        List<AbstractMessage> input = value instanceof List ? (List<AbstractMessage>) value : Collections.singletonList((AbstractMessage) value);

        Descriptors.FieldDescriptor keyDesc = fieldDescriptor.getMessageType().findFieldByNumber(1);
        Descriptors.FieldDescriptor valDesc = fieldDescriptor.getMessageType().findFieldByNumber(2);
        for (AbstractMessage entry : input) {
            Object keyValue = entry.getField(keyDesc);
            JavaValue keyJavaValue = new JavaValue(keyDesc, keyValue);

            Object valValue = entry.getField(valDesc);
            JavaValue valJavaValue = new JavaValue(valDesc, valValue);

            out.put(keyJavaValue, valJavaValue);
        }

        return out;
    }

}
