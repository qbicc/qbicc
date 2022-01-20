package org.qbicc.runtime.stdc;

import static org.qbicc.runtime.CNative.*;

import org.qbicc.runtime.Build;

/**
 *
 */
@include("<time.h>")
public class Time {
    public static final class time_t extends word {
    }

    public static final class time_t_ptr extends ptr<time_t> {}
    public static final class const_time_t_ptr extends ptr<@c_const time_t> {}

    public static final class clock_t extends object {
    }

    public static final class struct_timespec extends object {
        public time_t tv_sec;
        public c_long tv_nsec;
    }

    public static final class struct_timespec_ptr extends ptr<struct_timespec> {}
    public static final class const_struct_timespec_ptr extends ptr<@c_const struct_timespec> {}
    public static final class struct_timespec_ptr_ptr extends ptr<struct_timespec_ptr> {}
    public static final class const_struct_timespec_ptr_ptr extends ptr<const_struct_timespec_ptr> {}
    public static final class struct_timespec_ptr_const_ptr extends ptr<@c_const struct_timespec_ptr> {}
    public static final class const_struct_timespec_ptr_const_ptr extends ptr<@c_const const_struct_timespec_ptr> {}

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

    public static final class struct_tm_ptr extends ptr<struct_tm> {}
    public static final class const_struct_tm_ptr extends ptr<@c_const struct_tm> {}
    public static final class struct_tm_ptr_ptr extends ptr<struct_tm_ptr> {}
    public static final class const_struct_tm_ptr_ptr extends ptr<const_struct_tm_ptr> {}
    public static final class struct_tm_ptr_const_ptr extends ptr<@c_const struct_tm_ptr> {}
    public static final class const_struct_tm_ptr_const_ptr extends ptr<@c_const const_struct_tm_ptr> {}

    public static native time_t time(time_t_ptr timePtr);
}
