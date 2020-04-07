package cc.quarkus.c_native.linux;

import static cc.quarkus.c_native.api.CNative.*;
import static cc.quarkus.c_native.posix.SysTypes.*;
import static cc.quarkus.c_native.stdc.Stddef.*;

/**
 *
 */
@include("<sys/sendfile.h>")
public class SysSendfile {
    public static native ssize_t sendfile(c_int out_fd, c_int in_fd, ptr<off_t> offset, size_t count);
}
