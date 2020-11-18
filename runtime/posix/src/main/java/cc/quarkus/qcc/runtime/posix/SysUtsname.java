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

    public static native c_int uname(ptr<struct_utsname> buf);
}
