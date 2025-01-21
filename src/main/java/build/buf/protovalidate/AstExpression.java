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

import build.buf.protovalidate.exceptions.CompilationException;
import com.google.api.expr.v1alpha1.Type;
import org.projectnessie.cel.Ast;
import org.projectnessie.cel.Env;

/** {@link AstExpression} is a compiled CEL {@link Ast}. */
class AstExpression {
  /** The compiled CEL AST. */
  public final Ast ast;

  /** Contains the original expression from the proto file. */
  public final Expression source;

  /** Constructs a new {@link AstExpression}. */
  private AstExpression(Ast ast, Expression source) {
    this.ast = ast;
    this.source = source;
  }

  /**
   * Compiles the given expression to a {@link AstExpression}.
   *
   * @param env The CEL environment.
   * @param expr The expression to compile.
   * @return The compiled {@link AstExpression}.
   * @throws CompilationException if the expression compilation fails.
   */
  public static AstExpression newAstExpression(Env env, Expression expr)
      throws CompilationException {
    Env.AstIssuesTuple astIssuesTuple = env.compile(expr.expression);
    if (astIssuesTuple.hasIssues()) {
      throw new CompilationException(
          "Failed to compile expression " + expr.id + ":\n" + astIssuesTuple.getIssues());
    }
    Ast ast = astIssuesTuple.getAst();
    Type outType = ast.getResultType();
    if (outType.getPrimitive() != Type.PrimitiveType.BOOL
        && outType.getPrimitive() != Type.PrimitiveType.STRING) {
      throw new CompilationException(
          String.format(
              "Expression outputs, wanted either bool or string: %s %s", expr.id, outType));
    }
    return new AstExpression(ast, expr);
  }
}
