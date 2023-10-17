package org.qbicc.runtime.stdc;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stdc.Stddef.*;

import org.qbicc.runtime.NoReturn;
import org.qbicc.runtime.Build.Target.IsGLibC;
import org.qbicc.runtime.SafePoint;
import org.qbicc.runtime.SafePointBehavior;

/**
 *
 */
@include("<stdlib.h>")
@define(value = "_DEFAULT_SOURCE", when = IsGLibC.class)
public final class Stdlib {

    // heap

    @SafePoint(SafePointBehavior.ALLOWED)
    public static native <P extends ptr<?>> P malloc(size_t size);

    @SafePoint(SafePointBehavior.ALLOWED)
    public static native <P extends ptr<?>> P calloc(size_t nMembers, size_t size);

    @SafePoint(SafePointBehavior.ALLOWED)
    public static native <P extends ptr<?>> P realloc(ptr<?> ptr, size_t size);

    /**
     * Free the memory referenced by the given pointer.
     *
     * @param ptr a pointer containing the address of memory to free
     */
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native void free(ptr<?> ptr);

    // process exit

    @NoReturn
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native void abort();

    @NoReturn
    @SafePoint(SafePointBehavior.ENTER) // blocks forever
    public static native void exit(c_int exitCode);

    @SafePoint(SafePointBehavior.ALLOWED)
    public static native c_int atexit(ptr<function<Runnable>> function);

    // environment - thread unsafe wrt other env-related operations

    @SafePoint(SafePointBehavior.ALLOWED)
    public static native ptr<c_char> getenv(ptr<@c_const c_char> name);

    @SafePoint(SafePointBehavior.ALLOWED)
    public static native c_int setenv(ptr<@c_const c_char> name, ptr<@c_const c_char> value, c_int overwrite);

    @SafePoint(SafePointBehavior.ALLOWED)
    public static native c_int unsetenv(ptr<@c_const c_char> name);

    @SafePoint(SafePointBehavior.ALLOWED)
    public static native c_int clearenv();

    public static final c_int EXIT_SUCCESS = constant();
    public static final c_int EXIT_FAILURE = constant();

    @SafePoint(SafePointBehavior.ALLOWED)
    public static native c_long strtol(ptr<@c_const c_char> str, ptr<ptr<c_char>> entPtr, c_int base);
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native long_long strtoll(ptr<@c_const c_char> str, ptr<ptr<c_char>> entPtr, c_int base);
}
