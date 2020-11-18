package cc.quarkus.qcc.runtime.stdc;

import static cc.quarkus.qcc.runtime.api.CNative.*;

/**
 *
 */
@include("<limits.h>")
public final class Limits {
    public static final c_int ATEXIT_MAX = constant();

    public static final c_int IOV_MAX = constant();
}
