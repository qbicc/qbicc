package cc.quarkus.qcc.runtime.posix;

import static cc.quarkus.qcc.runtime.CNative.*;

/**
 *
 */
@include("<sys/utsname.h>")
public final class SysUtsname {
    public static final class struct_utsname extends object {
        public c_char[] sysname;
        public c_char[] nodename;
        public c_char[] release;
        public c_char[] version;
        public c_char[] machine;
    }

    public static final class struct_utsname_ptr extends ptr<struct_utsname> {}
    public static final class const_struct_utsname_ptr extends ptr<@c_const struct_utsname> {}
    public static final class struct_utsname_ptr_ptr extends ptr<struct_utsname_ptr> {}
    public static final class const_struct_utsname_ptr_ptr extends ptr<const_struct_utsname_ptr> {}
    public static final class struct_utsname_ptr_const_ptr extends ptr<@c_const struct_utsname_ptr> {}
    public static final class const_struct_utsname_ptr_const_ptr extends ptr<@c_const const_struct_utsname_ptr> {}


    public static native c_int uname(struct_utsname_ptr buf);
}
