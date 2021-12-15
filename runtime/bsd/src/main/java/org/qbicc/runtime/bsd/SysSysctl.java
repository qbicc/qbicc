package org.qbicc.runtime.bsd;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stdc.Stddef.*;

@include("<sys/sysctl.h>")
public class SysSysctl {
    public static native c_int sysctl(int_ptr name, unsigned_int nameLen, ptr<?> oldp, size_t_ptr oldlenp, ptr<?> newp, size_t newlen);

    public static final c_int CTL_DEBUG = constant();
    public static final c_int CTL_VFS = constant();
    public static final c_int CTL_HW = constant();
    public static final c_int CTL_KERN = constant();
    public static final c_int CTL_MACHDEP = constant();
    public static final c_int CTL_NET = constant();
    public static final c_int CTL_USER = constant();
    public static final c_int CTL_VM = constant();

    public static final c_int HW_NCPU = constant();
}


