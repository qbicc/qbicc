package org.qbicc.runtime.stdc;

import static org.qbicc.runtime.CNative.*;

/**
 *
 */
@include("<stdarg.h>")
public final class Stdarg {
    /**
     * The special type representing the platform-specific variable argument list.
     */
    public static final class va_list extends object {}

    public static final class va_list_ptr extends ptr<va_list> {}
    public static final class const_va_list_ptr extends ptr<@c_const va_list> {}
    public static final class va_list_ptr_ptr extends ptr<va_list_ptr> {}
    public static final class const_va_list_ptr_ptr extends ptr<const_va_list_ptr> {}
    public static final class va_list_ptr_const_ptr extends ptr<@c_const va_list_ptr> {}
    public static final class const_va_list_ptr_const_ptr extends ptr<@c_const const_va_list_ptr> {}

    /**
     * Start the variable argument processing.  May only be called from methods which have a final
     * variadic argument of type {@code object...}.
     *
     * @param ap the list to initialize
     */
    public static void va_start(va_list ap) {
        // macro replaced by intrinsic
        throw new UnsupportedOperationException();
    }

    public static <T extends object> T va_arg(va_list ap, Class<T> type) {
        // macro replaced by intrinsic
        throw new UnsupportedOperationException();
    }

    public static void va_end(va_list ap) {
        // macro replaced by intrinsic
        throw new UnsupportedOperationException();
    }

    public static void va_copy(va_list dest, va_list src) {
        // macro replaced by intrinsic
        throw new UnsupportedOperationException();
    }
}
