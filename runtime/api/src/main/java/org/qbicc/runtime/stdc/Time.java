package org.qbicc.runtime.stdc;

import static org.qbicc.runtime.CNative.*;

/**
 *
 */
@include("<time.h>")
public class Time {
    public static final class time_t extends word {
    }

    public static final class clock_t extends object {
    }

    public static final class struct_timespec extends object {
        public time_t tv_sec;
        public c_long tv_nsec;
    }

    public static final class struct_tm extends object {
        public c_int tm_sec;
        public c_int tm_min;
        public c_int tm_hour;
        public c_int tm_mday;
        public c_int tm_mon;
        public c_int tm_year;
        public c_int tm_wday;
        public c_int tm_yday;
        public c_int tm_isdst;

        // MacOS only
        public c_int tm_gmtoff;
    }

    public static native time_t time(ptr<time_t> timePtr);
}
