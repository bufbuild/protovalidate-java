package build.buf.protovalidate.internal.celext;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.junit.Test;
import org.projectnessie.cel.Ast;
import org.projectnessie.cel.Env;
import org.projectnessie.cel.Library;
import org.projectnessie.cel.Program;
import org.projectnessie.cel.interpreter.Activation;

public class CustomOverloadTest {

  private final Env env = Env.newEnv(Library.Lib(new ValidateLibrary()));

  @Test
  public void testIsInf() {
    Map<String, Boolean> testCases =
        ImmutableMap.<String, Boolean>builder()
            .put("0.0.isInf()", false)
            .put("(1.0/0.0).isInf()", true)
            .put("(1.0/0.0).isInf(0)", true)
            .put("(1.0/0.0).isInf(1)", true)
            .put("(1.0/0.0).isInf(-1)", false)
            .put("(-1.0/0.0).isInf()", true)
            .put("(-1.0/0.0).isInf(0)", true)
            .put("(-1.0/0.0).isInf(1)", false)
            .put("(-1.0/0.0).isInf(-1)", true)
            .build();
    for (Map.Entry<String, Boolean> testCase : testCases.entrySet()) {
      Program.EvalResult result = eval(testCase.getKey());
      assertThat(result.getVal().booleanValue()).isEqualTo(testCase.getValue());
    }
  }

  @Test
  public void testIsNan() {
    Map<String, Boolean> testCases =
        ImmutableMap.<String, Boolean>builder()
            .put("0.0.isNan()", false)
            .put("(0.0/0.0).isNan()", true)
            .put("(1.0/0.0).isNan()", false)
            .build();
    for (Map.Entry<String, Boolean> testCase : testCases.entrySet()) {
      Program.EvalResult result = eval(testCase.getKey());
      assertThat(result.getVal().booleanValue()).isEqualTo(testCase.getValue());
    }
  }

  private Program.EvalResult eval(String source) {
    return eval(source, Activation.emptyActivation());
  }

  private Program.EvalResult eval(String source, Object vars) {
    Env.AstIssuesTuple parsed = env.parse(source);
    assertThat(parsed.hasIssues()).isFalse();
    Ast ast = parsed.getAst();
    return env.program(ast).eval(vars);
  }
}
