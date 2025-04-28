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

import org.jspecify.annotations.Nullable;
import org.projectnessie.cel.interpreter.Activation;
import org.projectnessie.cel.interpreter.ResolvedValue;

/**
 * {@link Variable} implements {@link org.projectnessie.cel.interpreter.Activation}, providing a
 * lightweight named variable to cel.Program executions.
 */
class Variable implements Activation {
  /** The {@value} variable in CEL. */
  public static final String THIS_NAME = "this";

  /** The {@value} variable in CEL. */
  public static final String RULES_NAME = "rules";

  /** The {@value} variable in CEL. */
  public static final String RULE_NAME = "rule";

  /** The parent activation */
  private final Activation next;

  /** The variable's name */
  private final String name;

  /** The value for this variable */
  @Nullable private final Object val;

  /** Creates a variable with the given name and value. */
  private Variable(Activation activation, String name, @Nullable Object val) {
    this.next = activation;
    this.name = name;
    this.val = val;
  }

  /**
   * Creates a "this" variable.
   *
   * @param val the value.
   * @return {@link Variable}.
   */
  public static Variable newThisVariable(@Nullable Object val) {
    return new Variable(Activation.emptyActivation(), THIS_NAME, val);
    // return new Variable(new NowVariable(), THIS_NAME, val);
  }

  /**
   * Creates a "rules" variable.
   *
   * @param val the value.
   * @return {@link Variable}.
   */
  public static Variable newRulesVariable(Object val) {
    return new Variable(Activation.emptyActivation(), RULES_NAME, val);
  }

  /**
   * Creates a "rule" variable.
   *
   * @param rules the value of the "rules" variable.
   * @param val the value of the "rule" variable.
   * @return {@link Variable}.
   */
  public static Variable newRuleVariable(Object rules, Object val) {
    return new Variable(newRulesVariable(rules), RULE_NAME, val);
  }

  @Override
  public ResolvedValue resolveName(String name) {
    if (this.name.equals(name)) {
      return ResolvedValue.resolvedValue(val);
    } else if (next != null) {
      return next.resolveName(name);
    }
    return ResolvedValue.ABSENT;
  }

  @Override
  public Activation parent() {
    return next;
  }
}
