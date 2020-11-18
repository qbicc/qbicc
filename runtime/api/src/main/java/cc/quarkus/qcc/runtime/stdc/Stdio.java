package cc.quarkus.qcc.runtime.stdc;

import static cc.quarkus.qcc.runtime.api.CNative.*;

/**
 * C standard I/O should only be used for specialized debugging purposes.
 */
@include("<stdio.h>")
public final class Stdio {
    public static native c_int printf(@restrict ptr<@c_const c_char> format, object... args);

    public static native c_int fprintf(@restrict ptr<FILE> stream, @restrict ptr<@c_const c_char> format, object... args);

    @incomplete
    public static final class FILE extends object {
    }

    @define("_POSIX_C_SOURCE")
    public static native ptr<FILE> fdopen(c_int fd, ptr<@c_const c_char> mode);

    public static native c_int fclose(ptr<FILE> stream);

    public static final ptr<FILE> stdin = constant();
    public static final ptr<FILE> stdout = constant();
    public static final ptr<FILE> stderr = constant();
}
