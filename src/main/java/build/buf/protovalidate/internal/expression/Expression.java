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

package build.buf.protovalidate.internal.expression;

import build.buf.validate.priv.Constraint;
import java.util.ArrayList;
import java.util.List;

/** Expression represents a single CEL expression. */
public class Expression {
  /** The id of the constraint. */
  public final String id;

  /** The message of the constraint. */
  public final String message;

  /** The expression of the constraint. */
  public final String expression;

  /**
   * Constructs a new Expression.
   *
   * @param id The ID of the constraint.
   * @param message The message of the constraint.
   * @param expression The expression of the constraint.
   */
  private Expression(String id, String message, String expression) {
    this.id = id;
    this.message = message;
    this.expression = expression;
  }

  /**
   * Constructs a new Expression from the given constraint.
   *
   * @param constraint The constraint to create the expression from.
   */
  private Expression(build.buf.validate.Constraint constraint) {
    this(constraint.getId(), constraint.getMessage(), constraint.getExpression());
  }

  /**
   * Constructs a new Expression from the given private constraint.
   *
   * @param constraint The private constraint to create the expression from.
   */
  private Expression(build.buf.validate.priv.Constraint constraint) {
    this(constraint.getId(), constraint.getMessage(), constraint.getExpression());
  }

  /**
   * Constructs a new list of {@link Expression} from the given list of private constraints.
   *
   * @param constraints The list of private constraints.
   * @return The list of expressions.
   */
  public static List<Expression> fromPrivConstraints(
      List<build.buf.validate.priv.Constraint> constraints) {
    List<Expression> expressions = new ArrayList<>();
    for (Constraint constraint : constraints) {
      expressions.add(new Expression(constraint));
    }
    return expressions;
  }

  /**
   * Constructs a new list of {@link Expression} from the given list of constraints.
   *
   * @param constraints The list of constraints.
   * @return The list of expressions.
   */
  public static List<Expression> fromConstraints(List<build.buf.validate.Constraint> constraints) {
    List<Expression> expressions = new ArrayList<>();
    for (build.buf.validate.Constraint constraint : constraints) {
      expressions.add(new Expression(constraint));
    }
    return expressions;
  }
}
