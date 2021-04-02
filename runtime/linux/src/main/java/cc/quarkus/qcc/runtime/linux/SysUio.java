package cc.quarkus.qcc.runtime.linux;

import static cc.quarkus.qcc.runtime.CNative.*;
import static cc.quarkus.qcc.runtime.posix.SysTypes.*;
import static cc.quarkus.qcc.runtime.posix.SysUio.*;

/**
 *
 */
@include("<sys/uio.h>")
public final class SysUio {
    public static native ssize_t preadv(c_int fd, const_struct_iovec_ptr iov, c_int iovCnt, off_t offset);

    public static native ssize_t pwritev(c_int fd, const_struct_iovec_ptr iov, c_int iovCnt, off_t offset);

    public static native ssize_t preadv2(c_int fd, const_struct_iovec_ptr iov, c_int iovCnt, off_t offset, c_int flags);

    public static native ssize_t pwritev2(c_int fd, const_struct_iovec_ptr iov, c_int iovCnt, off_t offset, c_int flags);

    public static final c_int RWF_DSYNC = constant();
    public static final c_int RWF_HIPRI = constant();
    public static final c_int RWF_SYNC = constant();
    public static final c_int RWF_NOWAIT = constant();
    public static final c_int RWF_APPEND = constant();
}
