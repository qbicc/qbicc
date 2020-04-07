package cc.quarkus.c_native.linux;

import static cc.quarkus.c_native.api.CNative.*;
import static cc.quarkus.c_native.posix.SysTypes.*;
import static cc.quarkus.c_native.posix.SysUio.*;
import static cc.quarkus.c_native.stdc.Stddef.*;

/**
 *
 */
@define("_GNU_SOURCE")
@include("<fcntl.h>")
public class Fcntl {
    public static native ssize_t splice(c_int fd_in, ptr<loff_t> off_in, c_int fd_out, ptr<loff_t> off_out, size_t len,
            unsigned_int flags);

    public static native ssize_t vmsplice(c_int fd, ptr<@c_const struct_iovec> iov, unsigned_long nr_segs, unsigned_int flags);

    public static final c_int SPLICE_F_MOVE = constant();
    public static final c_int SPLICE_F_NONBLOCK = constant();
    public static final c_int SPLICE_F_MORE = constant();
    public static final c_int SPLICE_F_GIFT = constant();
}
