package org.qbicc.runtime.stdc;

import static org.qbicc.runtime.CNative.*;

/**
 * C standard I/O should only be used for specialized debugging purposes.
 */
@include("<stdio.h>")
public final class Stdio {
    public static native c_int printf(@restrict const_char_ptr format, object... args);

    public static native c_int fprintf(@restrict FILE_ptr stream, @restrict const_char_ptr format, object... args);

    @incomplete
    public static final class FILE extends object {
    }

    public static final class FILE_ptr extends ptr<FILE> {}
    public static final class const_FILE_ptr extends ptr<@c_const FILE> {}
    public static final class FILE_ptr_ptr extends ptr<FILE_ptr> {}
    public static final class const_FILE_ptr_ptr extends ptr<const_FILE_ptr> {}
    public static final class FILE_ptr_const_ptr extends ptr<@c_const FILE_ptr> {}
    public static final class const_FILE_ptr_const_ptr extends ptr<@c_const const_FILE_ptr> {}

    @define("_POSIX_C_SOURCE")
    public static native FILE_ptr fdopen(c_int fd, const_char_ptr mode);

    public static native c_int fclose(FILE_ptr stream);

    @extern
    public static FILE_ptr stdin;
    @extern
    public static FILE_ptr stdout;
    @extern
    public static FILE_ptr stderr;
}
