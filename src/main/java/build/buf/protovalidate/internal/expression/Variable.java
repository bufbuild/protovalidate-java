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

package build.buf.protovalidate.internal.expression;

import static org.projectnessie.cel.interpreter.ResolvedValue.ABSENT;

import javax.annotation.Nullable;
import org.projectnessie.cel.interpreter.Activation;
import org.projectnessie.cel.interpreter.ResolvedValue;

/**
 * {@link Variable} implements {@link org.projectnessie.cel.interpreter.Activation}, providing a
 * lightweight named variable to cel.Program executions.
 */
public class Variable implements Activation {
  public static final String THIS_NAME = "this";
  public static final String RULES_NAME = "rules";

  /** The parent activation */
  private final Activation next;

  /** The variable's name */
  private final String name;

  /** The value for this variable */
  @Nullable private final Object val;

  /** Creates a new variable with the given name and value. */
  private Variable(Activation activation, String name, @Nullable Object val) {
    this.next = activation;
    this.name = name;
    this.val = val;
  }

  /**
   * Creates a new "this" variable.
   *
   * @param val the value.
   * @return {@link Variable}.
   */
  public static Variable newThisVariable(@Nullable Object val) {
    return new Variable(Activation.emptyActivation(), THIS_NAME, val);
  }

  /**
   * Creates a new "rules" variable.
   *
   * @param val the value.
   * @return {@link Variable}.
   */
  public static Variable newRulesVariable(Object val) {
    return new Variable(new NowVariable(), RULES_NAME, val);
  }

  @Override
  public ResolvedValue resolveName(String name) {
    if (this.name.equals(name)) {
      return ResolvedValue.resolvedValue(val);
    } else if (next != null) {
      return next.resolveName(name);
    }
    return ABSENT;
  }

  @Override
  public Activation parent() {
    return next;
  }
}
