package cc.quarkus.c_native.posix;

import static cc.quarkus.qcc.runtime.api.CNative.*;
import static cc.quarkus.c_native.posix.SysTypes.*;
import static cc.quarkus.qcc.runtime.stdc.Stddef.*;

/**
 *
 */
@include(value = "<sys/uio.h>")
public final class SysUio {
    public static class struct_iovec extends object {
        public ptr<?> iov_base;
        public size_t length;
    }

    public static native ssize_t readv(c_int fd, ptr<@c_const struct_iovec> iov, c_int iov_cnt);

    public static native ssize_t writev(c_int fd, ptr<@c_const struct_iovec> iov, c_int iov_cnt);
}
