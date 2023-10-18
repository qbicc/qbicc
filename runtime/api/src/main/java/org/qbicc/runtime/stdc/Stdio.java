package org.qbicc.runtime.stdc;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stdc.Stddef.*;

import org.qbicc.runtime.Build;
import org.qbicc.runtime.SafePoint;
import org.qbicc.runtime.SafePointBehavior;

/**
 * C standard I/O should only be used for specialized debugging purposes.
 */
@include("<stdio.h>")
public final class Stdio {
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native c_int printf(@restrict ptr<@c_const c_char> format, object... args);

    @SafePoint(SafePointBehavior.ALLOWED)
    public static native c_int fprintf(@restrict ptr<FILE> stream, @restrict ptr<@c_const c_char> format, object... args);

    @SafePoint(SafePointBehavior.ALLOWED)
    public static native c_int snprintf(@restrict ptr<c_char> str, size_t size, @restrict ptr<@c_const c_char> format, object... args);

    public static native c_int fscanf(@restrict ptr<FILE> stream, ptr<@c_const c_char> format, object... args);
    public static native c_int scanf(@restrict ptr<@c_const c_char> format, object... args);
    public static native c_int sscanf(@restrict ptr<@c_const c_char> s, @restrict ptr<@c_const c_char> format, object... args);

    @incomplete
    public static final class FILE extends object {
    }

    @define("_POSIX_C_SOURCE")
    public static native ptr<FILE> fdopen(c_int fd, ptr<@c_const c_char> mode);

    public static native ptr<FILE> fopen(ptr<@c_const c_char> path, ptr<@c_const c_char> mode);

    public static native c_int fclose(ptr<FILE> stream);

    public static native size_t fread(@restrict ptr<?> ptr, size_t size, size_t nitems, ptr<FILE> stream);

    @extern
    @name(value = "__stdinp", when = Build.Target.IsMacOs.class)
    public static ptr<FILE> stdin;
    @extern
    @name(value = "__stdoutp", when = Build.Target.IsMacOs.class)
    public static ptr<FILE> stdout;
    @extern
    @name(value = "__stderrp", when = Build.Target.IsMacOs.class)
    public static ptr<FILE> stderr;

    @SafePoint(SafePointBehavior.ALLOWED)
    public static native c_int fflush(ptr<FILE> stream);
}
