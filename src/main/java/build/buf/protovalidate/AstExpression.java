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
import dev.cel.common.CelAbstractSyntaxTree;
import dev.cel.common.CelValidationException;
import dev.cel.common.CelValidationResult;
import dev.cel.common.types.CelKind;
import dev.cel.compiler.CelCompiler;

/** {@link AstExpression} is a compiled CEL {@link CelAbstractSyntaxTree}. */
final class AstExpression {
  /** The compiled CEL AST. */
  final CelAbstractSyntaxTree ast;

  /** Contains the original expression from the proto file. */
  final Expression source;

  /** Constructs a new {@link AstExpression}. */
  private AstExpression(CelAbstractSyntaxTree ast, Expression source) {
    this.ast = ast;
    this.source = source;
  }

  /**
   * Compiles the given expression to a {@link AstExpression}.
   *
   * @param cel The CEL compiler.
   * @param expr The expression to compile.
   * @return The compiled {@link AstExpression}.
   * @throws CompilationException if the expression compilation fails.
   */
  static AstExpression newAstExpression(CelCompiler cel, Expression expr)
      throws CompilationException {
    CelValidationResult compileResult = cel.compile(expr.expression);
    if (!compileResult.getAllIssues().isEmpty()) {
      throw new CompilationException(
          "Failed to compile expression " + expr.id + ":\n" + compileResult.getIssueString());
    }
    CelAbstractSyntaxTree ast;
    try {
      ast = compileResult.getAst();
    } catch (CelValidationException e) {
      // This will not happen as we checked for issues, and it only throws when
      // it has at least one issue of error severity.
      throw new CompilationException(
          "Failed to compile expression " + expr.id + ":\n" + compileResult.getIssueString());
    }
    CelKind outKind = ast.getResultType().kind();
    if (outKind != CelKind.BOOL && outKind != CelKind.STRING) {
      throw new CompilationException(
          String.format(
              "Expression outputs, wanted either bool or string: %s %s", expr.id, outKind));
    }
    return new AstExpression(ast, expr);
  }
}
