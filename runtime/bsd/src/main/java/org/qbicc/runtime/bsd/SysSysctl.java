package org.qbicc.runtime.bsd;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.bsd.SysProc.*;
import static org.qbicc.runtime.posix.SysTypes.*;
import static org.qbicc.runtime.stdc.Stddef.*;

@include("<sys/sysctl.h>")
public class SysSysctl {
    public static final class struct_kinfo_proc extends object {
        public struct_extern_proc kp_proc;
        public struct_eproc kp_eproc;
    }

    public static final class struct_eproc extends object {
        public pid_t e_ppid;
        public struct__ucred e_ucred;
    }

    public static final class struct__ucred extends object {
        public uid_t cr_uid;
    }

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

    public static final c_int KERN_ARGMAX = constant();
    public static final c_int KERN_IPC = constant();
    public static final c_int KERN_PROC = constant();
    public static final c_int KERN_PROC_PID = constant();
    public static final c_int KERN_PROCARGS2 = constant();

    public static final c_int KIPC_MAXSOCKBUF = constant();
}


