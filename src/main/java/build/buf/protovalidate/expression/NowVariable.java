package build.buf.protovalidate.expression;

import org.projectnessie.cel.common.types.TimestampT;
import org.projectnessie.cel.interpreter.Activation;
import org.projectnessie.cel.interpreter.ResolvedValue;

import java.time.Instant;

public class NowVariable implements Activation {
    private ResolvedValue resolvedValue;

    @Override
    public ResolvedValue resolveName(String name) {
        if (!name.equals("now")) {
            return ResolvedValue.ABSENT;
        } else if (resolvedValue != null) {
            return resolvedValue;
        }
        Instant instant = Instant.now();
        TimestampT value = TimestampT.timestampOf(instant);
        resolvedValue = ResolvedValue.resolvedValue(value);
        return resolvedValue;
    }

    @Override
    public Activation parent() {
        return Activation.emptyActivation();
    }
}
