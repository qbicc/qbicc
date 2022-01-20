package org.qbicc.runtime.posix;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stdc.Time.*;

/**
 *
 */
@include("<time.h>")
public final class Time {

    public static native c_int clock_gettime(clockid_t clockid, struct_timespec_ptr tp);

    public static final class clockid_t extends object {}
    public static final class clockid_t_ptr extends ptr<clockid_t> {}
    public static final class const_clockid_t_ptr extends ptr<@c_const clockid_t> {}

    public static final clockid_t CLOCK_REALTIME = constant();
    public static final clockid_t CLOCK_MONOTONIC = constant();

    @extern
    public static c_long timezone;

    public static native struct_tm_ptr localtime_r(const_time_t_ptr timePtr, struct_tm_ptr result);
}
