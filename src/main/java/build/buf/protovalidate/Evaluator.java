// Copyright 2023-2024 Buf Technologies, Inc.
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

import build.buf.protovalidate.exceptions.ExecutionException;

import java.util.List;

/**
 * {@link Evaluator} defines a validation evaluator. evaluator implementations may elide type
 * checking of the passed in value, as the types have been guaranteed during the build phase.
 */
interface Evaluator {
  /**
   * Tautology returns true if the evaluator always succeeds.
   *
   * @return True if the evaluator always succeeds.
   */
  boolean tautology();

  /**
   * Checks that the provided val is valid. Unless failFast is true, evaluation attempts to find all
   * {@link ConstraintViolation} present in val instead of returning only the first {@link
   * ConstraintViolation}.
   *
   * @param val The value to validate.
   * @param failFast If true, validation stops after the first failure.
   * @return The result of validation on the specified value.
   * @throws ExecutionException If evaluation fails to complete.
   */
  List<ConstraintViolation.Builder> evaluate(Value val, boolean failFast) throws ExecutionException;
}
