package cc.quarkus.qcc.runtime.posix;

import static cc.quarkus.qcc.runtime.CNative.*;
import static cc.quarkus.qcc.runtime.posix.SysTypes.*;
import static cc.quarkus.qcc.runtime.stdc.Stddef.*;

/**
 *
 */
@include(value = "<sys/uio.h>")
public final class SysUio {
    public static class struct_iovec extends object {
        public void_ptr iov_base;
        public size_t length;
    }

    public static final class struct_iovec_ptr extends ptr<struct_iovec> {}
    public static final class struct_iovec_ptr_ptr extends ptr<struct_iovec_ptr> {}
    public static final class struct_iovec_ptr_const_ptr extends ptr<@c_const struct_iovec_ptr> {}
    public static final class const_struct_iovec_ptr extends ptr<@c_const struct_iovec> {}
    public static final class const_struct_iovec_ptr_ptr extends ptr<const_struct_iovec_ptr> {}
    public static final class const_struct_iovec_ptr_const_ptr extends ptr<@c_const const_struct_iovec_ptr> {}

    public static native ssize_t readv(c_int fd, const_struct_iovec_ptr iov, c_int iov_cnt);

    public static native ssize_t writev(c_int fd, const_struct_iovec_ptr iov, c_int iov_cnt);
}
