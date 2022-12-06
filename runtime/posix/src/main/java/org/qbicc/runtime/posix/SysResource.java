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
}
