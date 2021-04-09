package org.qbicc.runtime.stdc;

import static org.qbicc.runtime.CNative.*;

/**
 *
 */
@include("<limits.h>")
public final class Limits {
    public static final c_int ATEXIT_MAX = constant();

    public static final c_int IOV_MAX = constant();
}
