package cc.quarkus.qcc.runtime.linux;

import static cc.quarkus.qcc.runtime.CNative.*;
import static cc.quarkus.qcc.runtime.posix.SysTypes.*;
import static cc.quarkus.qcc.runtime.stdc.Stddef.*;

/**
 *
 */
@include("<sys/sendfile.h>")
public class SysSendfile {
    public static native ssize_t sendfile(c_int out_fd, c_int in_fd, off_t_ptr offset, size_t count);
}
