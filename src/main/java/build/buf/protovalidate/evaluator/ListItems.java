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
import com.google.protobuf.ListValue;

import java.util.Collections;

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
    public ValidationResult evaluate(JavaValue val, boolean failFast) {
        // TODO: who passes down this item?
//        ListValue listValue = val.getListValue();
//        boolean failed = false;
//        for (com.google.protobuf.Value value : listValue.getValuesList()) {
//            ValidationResult evaluate = itemConstraints.evaluate(value, failFast);
//            // Aggregate errors here. For now we dont.
//            if (evaluate.isFailure()) {
//                failed = true;
//                // TODO: violation string prefix error paths
//            }
////            ErrorUtils.merge()
//        }
//        if (failed) {
//            // TODO: make this right
//            return new ValidationResult(new ValidationError(Collections.singletonList(Violation.newBuilder().build())));
//        }
        return ValidationResult.success();
    }

    @Override
    public void append(Evaluator eval) {
        throw new UnsupportedOperationException("append not supported for ListItems");
    }
}
