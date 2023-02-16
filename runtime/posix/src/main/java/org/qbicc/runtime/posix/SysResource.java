package org.qbicc.runtime.posix;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.posix.SysTime.*;

import org.qbicc.runtime.Build;

@include(value = "<sys/resource.h>", when = Build.Target.IsPosix.class)
@include(value = "<sys/time.h>", when = Build.Target.IsPosix.class)
public class SysResource {
    public static final class struct_rusage extends object {
        public struct_timeval ru_utime;
        public struct_timeval ru_stime;
    }

    public static final c_int RUSAGE_SELF = constant();
    public static final c_int RUSAGE_CHILDREN = constant();

    public static native c_int getrusage(c_int who, ptr<struct_rusage> r_usage);

    public static final class rlim_t extends word {}

    public static final rlim_t RLIM_INFINITY = constant();
    public static final rlim_t RLIM_SAVED_MAX = constant();
    public static final rlim_t RLIM_SAVED_CUR = constant();

    public static final class struct_rlimit extends object {
        public rlim_t rlim_cur;
        public rlim_t rlim_max;
    }

    public static final c_int RLIMIT_CORE = constant();
    public static final c_int RLMIT_CPU = constant();
    public static final c_int RLIMIT_DATA = constant();
    public static final c_int RLIMIT_FSIZE = constant();
    public static final c_int RLIMIT_NOFILE = constant();
    public static final c_int RLIMIT_STACK = constant();
    public static final c_int RLIMIT_AS = constant();

    public static native c_int getrlimit(c_int resource, ptr<struct_rlimit> rlp);
}
