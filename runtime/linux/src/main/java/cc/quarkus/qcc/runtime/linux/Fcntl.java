package org.qbicc.runtime.linux;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.posix.SysTypes.*;
import static org.qbicc.runtime.posix.SysUio.*;
import static org.qbicc.runtime.stdc.Stddef.*;

/**
 *
 */
@define("_GNU_SOURCE")
@include("<fcntl.h>")
public class Fcntl {
    public static native ssize_t splice(c_int fd_in, loff_t_ptr off_in, c_int fd_out, loff_t_ptr off_out, size_t len,
            unsigned_int flags);

    public static native ssize_t vmsplice(c_int fd, const_struct_iovec_ptr iov, unsigned_long nr_segs, unsigned_int flags);

    public static final c_int SPLICE_F_MOVE = constant();
    public static final c_int SPLICE_F_NONBLOCK = constant();
    public static final c_int SPLICE_F_MORE = constant();
    public static final c_int SPLICE_F_GIFT = constant();
}
