package org.qbicc.runtime.linux;

import static org.qbicc.runtime.CNative.*;

@SuppressWarnings("SpellCheckingInspection")
@include("<sys/ioctl.h>")
public class SysIoctl {

    public static final unsigned_long BLKGETSIZE64 = constant();
    public static final unsigned_long SIOCGIFINDEX = constant();
}
