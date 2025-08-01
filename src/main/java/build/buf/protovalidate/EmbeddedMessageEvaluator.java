// Copyright 2023-2025 Buf Technologies, Inc.
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
import java.util.Collections;
import java.util.List;

final class EmbeddedMessageEvaluator implements Evaluator {
  private final RuleViolationHelper helper;
  private final MessageEvaluator messageEvaluator;

  EmbeddedMessageEvaluator(ValueEvaluator valueEvaluator, MessageEvaluator messageEvaluator) {
    this.helper = new RuleViolationHelper(valueEvaluator);
    this.messageEvaluator = messageEvaluator;
  }

  @Override
  public boolean tautology() {
    return messageEvaluator.tautology();
  }

  @Override
  public List<RuleViolation.Builder> evaluate(Value val, boolean failFast)
      throws ExecutionException {
    return FieldPathUtils.updatePaths(
        messageEvaluator.evaluate(val, failFast),
        helper.getFieldPathElement(),
        Collections.emptyList());
  }
}
