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
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ListItems implements Evaluator {

    // ItemConstraints are checked on every item of the list
    private Value itemConstraints;

    public ListItems() {
        new ListItems(new Value());
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
                list.addAll((List<DynamicMessage>) value);
            }
        }
        for (int i = 0; i < list.size(); i++) {
            DynamicMessage item = list.get(i);
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
