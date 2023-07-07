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

import build.buf.protovalidate.results.ExecutionException;
import build.buf.protovalidate.results.ValidationResult;
import com.google.protobuf.Message;

/**
 * Essentially the same as evaluator, but specialized for
 * messages as an optimization. See {@link Evaluator} for behavior.
 */
public interface MessageEvaluator extends Evaluator {
    /**
     * Checks that the provided msg is valid. See {@link Evaluator} for behavior
     */
    ValidationResult evaluateMessage(Message val, boolean failFast) throws ExecutionException;
}
