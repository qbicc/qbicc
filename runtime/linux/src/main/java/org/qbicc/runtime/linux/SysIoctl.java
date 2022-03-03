package org.qbicc.runtime.linux;

import static org.qbicc.runtime.CNative.*;

@SuppressWarnings("SpellCheckingInspection")
@include("<sys/ioctl.h>")
public class SysIoctl {

    public static native c_int ioctl(c_int fd, unsigned_long request, object... more);

    public static final unsigned_long BLKGETSIZE64 = constant();
}
