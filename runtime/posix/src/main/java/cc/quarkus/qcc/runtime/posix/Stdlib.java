package cc.quarkus.qcc.runtime.posix;

import static cc.quarkus.qcc.runtime.CNative.*;
import static cc.quarkus.qcc.runtime.stdc.Stddef.*;

/**
 *
 */
@include("<stdlib.h>")
@define(value = "_POSIX_C_SOURCE", as = "200809L")
public final class Stdlib {
    // heap

    public static native c_int posix_memalign(ptr<ptr<?>> memPtr, size_t alignment, size_t size);
}
