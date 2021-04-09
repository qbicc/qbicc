package org.qbicc.runtime.linux;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.posix.SysTypes.*;
import static org.qbicc.runtime.stdc.Stddef.*;

/**
 *
 */
@include("<sys/sendfile.h>")
public class SysSendfile {
    public static native ssize_t sendfile(c_int out_fd, c_int in_fd, off_t_ptr offset, size_t count);
}
