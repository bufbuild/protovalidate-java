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

import build.buf.validate.Rule;
import java.util.ArrayList;
import java.util.List;

/** Expression represents a single CEL expression. */
final class Expression {
  /** The id of the rule. */
  final String id;

  /** The message of the rule. */
  final String message;

  /** The expression of the rule. */
  final String expression;

  /**
   * Constructs a new Expression.
   *
   * @param id The ID of the rule.
   * @param message The message of the rule.
   * @param expression The expression of the rule.
   */
  private Expression(String id, String message, String expression) {
    this.id = id;
    this.message = message;
    this.expression = expression;
  }

  /**
   * Constructs a new Expression from the given rule.
   *
   * @param rule The rule to create the expression from.
   */
  private Expression(Rule rule) {
    this(rule.getId(), rule.getMessage(), rule.getExpression());
  }

  /**
   * Constructs a new list of {@link Expression} from the given list of rules.
   *
   * @param rules The list of rules.
   * @return The list of expressions.
   */
  static List<Expression> fromRules(List<build.buf.validate.Rule> rules) {
    List<Expression> expressions = new ArrayList<>();
    for (build.buf.validate.Rule rule : rules) {
      expressions.add(new Expression(rule));
    }
    return expressions;
  }
}
