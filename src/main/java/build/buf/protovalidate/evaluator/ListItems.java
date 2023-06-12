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
import com.google.protobuf.DynamicMessage;

import java.util.ArrayList;
import java.util.List;

public class ListItems implements Evaluator {

    // ItemConstraints are checked on every item of the list
    public final Value itemConstraints;

    public ListItems() {
        this(new Value());
    }
    public ListItems(Value itemConstraints) {
        this.itemConstraints = itemConstraints;
    }

    @Override
    public boolean tautology() {
        return itemConstraints.tautology();
    }

    @Override
    public ValidationResult evaluate(DynamicMessage val, boolean failFast) {
        List<DynamicMessage> list = new ArrayList<>();
        for (Object value : val.getAllFields().values()) {
            if (value instanceof List) {
                List fieldValueList = (List) value;
                if (fieldValueList.isEmpty()) {
                    continue;
                }
                Object elm = fieldValueList.get(0);
                if (elm instanceof DynamicMessage) {
                    list.addAll((List<DynamicMessage>) value);
                }
            }
        }
        for (DynamicMessage item : list) {
            ValidationResult evaluate = itemConstraints.evaluate(item, failFast);
            if (evaluate.isFailure()) {
                return evaluate;
            }
        }
        return ValidationResult.success();
    }

    @Override
    public void append(Evaluator eval) {
        throw new UnsupportedOperationException("append not supported for ListItems");
    }
}
