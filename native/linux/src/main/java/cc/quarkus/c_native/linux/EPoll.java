package cc.quarkus.c_native.linux;

import static cc.quarkus.qcc.runtime.api.CNative.*;
import static cc.quarkus.qcc.runtime.stdc.Signal.*;
import static cc.quarkus.qcc.runtime.stdc.Stdint.*;

/**
 *
 */
@include("<sys/epoll.h>")
public class EPoll {

    public static native c_int epoll_create(c_int size);

    public static native c_int epoll_create1(c_int flags);

    public static native c_int epoll_ctl(c_int epfd, c_int op, c_int fd, ptr<struct_epoll_event> event);

    public static native c_int epoll_wait(c_int epfd, ptr<struct_epoll_event> events, c_int maxEvents, c_int timeout);

    public static native c_int epoll_pwait(c_int epfd, ptr<struct_epoll_event> events, c_int maxEvents, c_int timeout,
            ptr<@c_const sigset_t> sigMask);

    public static final c_int EPOLL_CLOEXEC = constant();

    public static final c_int EPOLL_CTL_ADD = constant();
    public static final c_int EPOLL_CTL_MOD = constant();
    public static final c_int EPOLL_CTL_DEL = constant();

    public static final uint32_t EPOLLIN = constant();
    public static final uint32_t EPOLLOUT = constant();
    public static final uint32_t EPOLLRDHUP = constant();
    public static final uint32_t EPOLLPRI = constant();
    public static final uint32_t EPOLLERR = constant();
    public static final uint32_t EPOLLHUP = constant();
    public static final uint32_t EPOLLET = constant();
    public static final uint32_t EPOLLONESHOT = constant();
    public static final uint32_t EPOLLWAKEUP = constant();
    public static final uint32_t EPOLLEXCLUSIVE = constant();

    public static final class /* union */ epoll_data_t extends object {
        public ptr<?> ptr;
        public c_int fd;
        public uint32_t u32;
        public uint64_t u64;
    }

    public static final class struct_epoll_event extends object {
        public uint32_t events;
        public epoll_data_t data;
    }
}
