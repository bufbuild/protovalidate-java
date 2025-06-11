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

import dev.cel.runtime.CelVariableResolver;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * {@link Variable} implements {@link CelVariableResolver}, providing a lightweight named variable
 * to cel.Program executions.
 */
final class Variable implements CelVariableResolver {
  /** The {@value} variable in CEL. */
  static final String THIS_NAME = "this";

  /** The {@value} variable in CEL. */
  static final String RULES_NAME = "rules";

  /** The {@value} variable in CEL. */
  static final String RULE_NAME = "rule";

  /** The variable's name */
  private final String name;

  /** The value for this variable */
  @Nullable private final Object val;

  /** Creates a variable with the given name and value. */
  private Variable(String name, @Nullable Object val) {
    this.name = name;
    this.val = val;
  }

  /**
   * Creates a "this" variable.
   *
   * @param val the value.
   * @return {@link Variable}.
   */
  static CelVariableResolver newThisVariable(@Nullable Object val) {
    return CelVariableResolver.hierarchicalVariableResolver(
        new NowVariable(), new Variable(THIS_NAME, val));
  }

  /**
   * Creates a "rules" variable.
   *
   * @param val the value.
   * @return {@link Variable}.
   */
  static CelVariableResolver newRulesVariable(Object val) {
    return new Variable(RULES_NAME, val);
  }

  /**
   * Creates a "rule" variable.
   *
   * @param rules the value of the "rules" variable.
   * @param val the value of the "rule" variable.
   * @return {@link Variable}.
   */
  static CelVariableResolver newRuleVariable(Object rules, Object val) {
    return CelVariableResolver.hierarchicalVariableResolver(
        newRulesVariable(rules), new Variable(RULE_NAME, val));
  }

  @Override
  public Optional<Object> find(String name) {
    if (!this.name.equals(name) || val == null) {
      return Optional.empty();
    }
    return Optional.of(val);
  }
}
