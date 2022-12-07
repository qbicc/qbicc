package org.qbicc.runtime.posix;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.posix.SysTypes.*;
import static org.qbicc.runtime.stdc.Stddef.*;

@include("<pwd.h>")
@define(value = "_POSIX_C_SOURCE", as = "200809L")
public class Pwd {
    public static final class struct_passwd extends object {
        public char_ptr pw_name;
        public uid_t pw_uid;
    }
    public static final class struct_passwd_ptr extends ptr<struct_passwd> {}

    public static native c_int getpwuid_r(uid_t uuid, ptr<struct_passwd> pwd, char_ptr buffer, size_t bufsize, ptr<ptr<struct_passwd>> result);
}
