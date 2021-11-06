package org.qbicc.runtime.linux;

import static org.qbicc.runtime.CNative.*;

/**
 *
 */
@include("<sys/syscall.h>")
public class SysSyscall {
    public static native c_long syscall(c_long number, object... args);

    public static final c_long SYS_futex = constant();
}
