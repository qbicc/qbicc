package cc.quarkus.qcc.runtime.posix;

import static cc.quarkus.qcc.runtime.CNative.*;
import static cc.quarkus.qcc.runtime.posix.SysTypes.*;

/**
 *
 */
@define(value = "_POSIX_C_SOURCE", as = "200809L")
@include("<fcntl.h>")
public class Fcntl {
    public static native c_int open(ptr<@c_const c_char> pathname, c_int flags);

    public static native c_int open(ptr<@c_const c_char> pathname, c_int flags, mode_t mode);

    public static native c_int openat(c_int dirFd, ptr<@c_const c_char> pathname, c_int flags);

    public static native c_int openat(c_int dirFd, ptr<@c_const c_char> pathname, c_int flags, mode_t mode);
}
