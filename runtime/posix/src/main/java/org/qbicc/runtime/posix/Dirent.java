package org.qbicc.runtime.posix;

import static org.qbicc.runtime.CNative.*;

@include("<dirent.h>")
@define(value = "_POSIX_C_SOURCE", as = "200809L")
public class Dirent {
    @incomplete
    public static final class DIR extends object {
    }
    public static final class DIR_ptr extends ptr<DIR> {}

    public static native c_int closedir(DIR_ptr dir);
    public static native DIR_ptr fdopendir(c_int fd);
    public static native DIR_ptr opendir(const_char_ptr path);
    public static native SysDirent.struct_dirent_ptr readdir(DIR_ptr dir);
}
