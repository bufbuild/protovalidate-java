package build.buf.protovalidatejava;

import build.buf.validate.python.Simple;
import org.junit.Test;

public class ValidateTest {
    @Test
    public void asdf() {
        Simple simple = Simple.newBuilder()
                .setX(2)
                .build();
        Validate validate = new Validate();
        validate.validate(simple);
    }
}