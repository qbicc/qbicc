package org.qbicc.runtime.posix;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stdc.Stddef.*;

/**
 *
 */
@include("<string.h>")
@define(value = "_POSIX_C_SOURCE", as = "200809L")
public final class String {
    public static native c_int strerror_r(c_int errno, ptr<c_char> buf, size_t bufLen);

    // GLIBC only
    public static native c_int __xpg_strerror_r(c_int errno, ptr<c_char> buf, size_t bufLen);
}
