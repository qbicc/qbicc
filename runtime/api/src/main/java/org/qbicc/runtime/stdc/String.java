package org.qbicc.runtime.stdc;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stdc.Stddef.*;

/**
 *
 */
@SuppressWarnings("SpellCheckingInspection")
@include("<string.h>")
public class String {
    public static native size_t strlen(ptr<@c_const c_char> s);

    public static native ptr<c_char> strerror(c_int errNum);

    public static native ptr<?> memcpy(ptr<?> dest, @restrict ptr<@c_const ?> src, size_t n);
    public static native ptr<?> memmove(ptr<?> dest, ptr<@c_const ?> src, size_t n);

    public static native ptr<?> memset(ptr<?> dest, c_int data, size_t len);

    public static native c_int memcmp(ptr<@c_const ?> src1, ptr<@c_const ?> src2, size_t len);

    public static native c_int strcmp(ptr<@c_const c_char> src1, ptr<@c_const c_char> src2);

    public static native c_int strncmp(ptr<@c_const c_char> src1, ptr<@c_const c_char> src2, size_t len);

    public static native c_int strncpy(ptr<c_char> dst, ptr<@c_const c_char> src, size_t len);

    public static native ptr<c_char> strncat(@restrict ptr<c_char> s1, @restrict ptr<c_char> s2, size_t n);

    public static native ptr<c_char> strchr(ptr<@c_const c_char> s, c_int c);

    public static native ptr<c_char> strrchr(ptr<@c_const c_char> s, c_int c);
}
