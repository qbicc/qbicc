package cc.quarkus.qcc.runtime.stdc;

import static cc.quarkus.qcc.runtime.api.CNative.*;

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
}
