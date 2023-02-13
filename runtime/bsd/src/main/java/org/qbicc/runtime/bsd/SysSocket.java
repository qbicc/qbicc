package org.qbicc.runtime.bsd;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.posix.SysTypes.*;

/**
 *
 */
@include("<sys/socket.h>")
public class SysSocket {

    public static final class struct_sf_hdtr extends object { }

    public static native c_int sendfile(c_int out_fd, c_int in_fd, off_t offset, off_t_ptr len, ptr<struct_sf_hdtr> hdtr, c_int flags);
}
