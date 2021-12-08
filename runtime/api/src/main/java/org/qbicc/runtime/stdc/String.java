package org.qbicc.runtime.stdc;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stdc.Stddef.*;

/**
 *
 */
@SuppressWarnings("SpellCheckingInspection")
@include("<string.h>")
public class String {
    public static native size_t strlen(const_char_ptr s);

    public static native char_ptr strerror(c_int errNum);

    public static native void_ptr memcpy(void_ptr dest, const_void_ptr src, size_t n);

    public static native void_ptr memset(void_ptr dest, c_int data, size_t len);

    public static native c_int memcmp(const_void_ptr src1, const_void_ptr src2, size_t len);
}
