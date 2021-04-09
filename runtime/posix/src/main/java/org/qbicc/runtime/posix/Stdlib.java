package org.qbicc.runtime.posix;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stdc.Stddef.*;

/**
 *
 */
@include("<stdlib.h>")
@define(value = "_POSIX_C_SOURCE", as = "200809L")
public final class Stdlib {
    // heap

    public static native c_int posix_memalign(void_ptr_ptr memPtr, size_t alignment, size_t size);
}
