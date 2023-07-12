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

/**
 * {@link Evaluator} defines a validation evaluator. evaluator implementations may elide
 * type checking of the passed in value, as the types have been guaranteed
 * during the build phase.
 */
public interface Evaluator {
    /**
     * Returns true if the evaluator always succeeds.
     */
    boolean tautology();

    /**
     * Checks that the provided val is valid. Unless failFast is true,
     * evaluation attempts to find all {@link build.buf.gen.buf.validate.Violations} present in val instead of
     * returning a {@link ValidationResult} on the first {@link build.buf.gen.buf.validate.Violation}.
     * An {@link ExecutionException} is thrown if evaluation fails to complete.
     */
    ValidationResult evaluate(Value val, boolean failFast) throws ExecutionException;

    /**
     * Appends the given {@link Evaluator} to this {@link Evaluator}.
     */
    void append(Evaluator eval);
}




