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

    public static final class struct_timespec_ptr extends ptr<struct_timespec> {}
    public static final class const_struct_timespec_ptr extends ptr<@c_const struct_timespec> {}
    public static final class struct_timespec_ptr_ptr extends ptr<struct_timespec_ptr> {}
    public static final class const_struct_timespec_ptr_ptr extends ptr<const_struct_timespec_ptr> {}
    public static final class struct_timespec_ptr_const_ptr extends ptr<@c_const struct_timespec_ptr> {}
    public static final class const_struct_timespec_ptr_const_ptr extends ptr<@c_const const_struct_timespec_ptr> {}
}
