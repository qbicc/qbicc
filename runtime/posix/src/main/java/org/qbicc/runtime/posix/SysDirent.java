package org.qbicc.runtime.posix;

import static org.qbicc.runtime.CNative.*;

@include("<sys/dirent.h>")
@define(value = "_POSIX_C_SOURCE", as = "200809L")
public class SysDirent {
    public static final class struct_dirent extends object {
        public c_char[] d_name;
    }
    public static final class struct_dirent_ptr extends ptr<struct_dirent> {}
}
