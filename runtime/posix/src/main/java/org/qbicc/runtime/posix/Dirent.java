package org.qbicc.runtime.posix;

import org.qbicc.runtime.Build;

import static org.qbicc.runtime.CNative.*;

@include("<dirent.h>")
@include(value = "<sys/dirent.h>", when = Build.Target.IsMacOs.class)
@define(value = "_POSIX_C_SOURCE", as = "200809L")
public class Dirent {
    @incomplete
    public static final class DIR extends object {
    }
    public static final class DIR_ptr extends ptr<DIR> {}

    public static final class struct_dirent extends object {
        public c_char[] d_name;
    }
    public static final class struct_dirent_ptr extends ptr<struct_dirent> {}

    public static native c_int closedir(DIR_ptr dir);
    public static native DIR_ptr fdopendir(c_int fd);
    public static native DIR_ptr opendir(const_char_ptr path);
    public static native struct_dirent_ptr readdir(DIR_ptr dir);
}
