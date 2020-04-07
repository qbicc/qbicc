package cc.quarkus.c_native.linux;

import static cc.quarkus.c_native.api.CNative.*;

/**
 *
 */
@include("<sys/eventfd.h>")
public class EventFD {
    public static native c_int eventfd(unsigned_int initVal, c_int flags);

    public static final c_int EFD_CLOEXEC = constant();
    public static final c_int EFD_NONBLOCK = constant();
    public static final c_int EFD_SEMAPHORE = constant();
}
