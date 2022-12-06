package org.qbicc.runtime.posix;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.posix.SysTypes.*;
import static org.qbicc.runtime.stdc.Time.*;


import org.qbicc.runtime.Build;

@include(value = "<sys/time.h>", when = Build.Target.IsPosix.class)
public class SysTime {

    public static final class struct_timeval extends object {
        public time_t tv_sec;
        public suseconds_t tv_usec;
    }
    public static final class struct_timeval_ptr extends ptr<struct_timeval> {}
}
