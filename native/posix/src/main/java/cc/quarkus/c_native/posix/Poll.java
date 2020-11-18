package cc.quarkus.c_native.posix;

import static cc.quarkus.qcc.runtime.api.CNative.*;
import static cc.quarkus.qcc.runtime.stdc.Signal.*;
import static cc.quarkus.qcc.runtime.stdc.Time.*;

/**
 *
 */
@include("<poll.h>")
public final class Poll {

    public static native c_int poll(ptr<struct_pollfd> fds, nfds_t nfds, c_int timeout);

    @define("_GNU_SOURCE")
    public static native c_int ppoll(ptr<struct_pollfd> fds, nfds_t nfds, ptr<@c_const struct_timespec> tmo_p,
            ptr<@c_const sigset_t> sigMask);

    public static final class nfds_t extends word {
    }

    public static final class struct_pollfd extends object {
        public c_int fd;
        public c_short events;
        public c_short revents;
    }

    public static final c_short POLLIN = constant();
    public static final c_short POLLPRI = constant();
    public static final c_short POLLOUT = constant();
    public static final c_short POLLRDHUP = constant();
    public static final c_short POLLERR = constant();
    public static final c_short POLLHUP = constant();
    public static final c_short POLLNVAL = constant();

}
