package build.buf.protovalidate;

import build.buf.protovalidate.evaluator.ConstraintResolver;
import com.google.protobuf.Descriptors;

import java.util.Collections;
import java.util.List;

public class Config {
    public final boolean failFast;
    public final boolean useUTC;
    public final boolean disableLazy;
    public final List<Descriptors.Descriptor> desc;
    public final ConstraintResolver resolver;

    public Config(boolean failFast, boolean useUTC, boolean disableLazy, List<Descriptors.Descriptor> desc, ConstraintResolver resolver) {
        this.failFast = failFast;
        this.useUTC = useUTC;
        this.disableLazy = disableLazy;
        this.desc = desc;
        this.resolver = resolver;
    }

    public Config() {
        this(false, true, true, Collections.emptyList(), new ConstraintResolver());
    }
}
