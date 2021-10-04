package org.qbicc.runtime.posix;

import static org.qbicc.runtime.CNative.*;

/**
 *
 */
@include("<sched.h>")
@define(value = "_POSIX_C_SOURCE", as = "200809L")
public class Sched {
    public static native c_int sched_yield();
}
