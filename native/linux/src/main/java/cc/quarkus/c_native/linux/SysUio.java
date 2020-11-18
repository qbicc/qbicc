package cc.quarkus.c_native.linux;

import static cc.quarkus.qcc.runtime.api.CNative.*;
import static cc.quarkus.c_native.posix.SysTypes.*;
import static cc.quarkus.c_native.posix.SysUio.*;

/**
 *
 */
@include("<sys/uio.h>")
public final class SysUio {
    public static native ssize_t preadv(c_int fd, ptr<@c_const struct_iovec> iov, c_int iovCnt, off_t offset);

    public static native ssize_t pwritev(c_int fd, ptr<@c_const struct_iovec> iov, c_int iovCnt, off_t offset);

    public static native ssize_t preadv2(c_int fd, ptr<@c_const struct_iovec> iov, c_int iovCnt, off_t offset, c_int flags);

    public static native ssize_t pwritev2(c_int fd, ptr<@c_const struct_iovec> iov, c_int iovCnt, off_t offset, c_int flags);

    public static final c_int RWF_DSYNC = constant();
    public static final c_int RWF_HIPRI = constant();
    public static final c_int RWF_SYNC = constant();
    public static final c_int RWF_NOWAIT = constant();
    public static final c_int RWF_APPEND = constant();
}
