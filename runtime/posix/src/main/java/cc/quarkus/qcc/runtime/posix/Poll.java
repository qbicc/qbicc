package org.qbicc.runtime.posix;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stdc.Signal.*;
import static org.qbicc.runtime.stdc.Time.*;

/**
 *
 */
@include("<poll.h>")
public final class Poll {

    public static native c_int poll(struct_pollfd_ptr fds, nfds_t nfds, c_int timeout);

    @define("_GNU_SOURCE")
    public static native c_int ppoll(struct_pollfd_ptr fds, nfds_t nfds, const_struct_timespec_ptr tmo_p,
            const_sigset_t_ptr sigMask);

    public static final class nfds_t extends word {
    }

    public static final class struct_pollfd extends object {
        public c_int fd;
        public c_short events;
        public c_short revents;
    }

    public static final class struct_pollfd_ptr extends ptr<struct_pollfd> {}
    public static final class const_struct_pollfd_ptr extends ptr<@c_const struct_pollfd> {}
    public static final class struct_pollfd_ptr_ptr extends ptr<struct_pollfd_ptr> {}
    public static final class const_struct_pollfd_ptr_ptr extends ptr<const_struct_pollfd_ptr> {}
    public static final class struct_pollfd_ptr_const_ptr extends ptr<@c_const struct_pollfd_ptr> {}
    public static final class const_struct_pollfd_ptr_const_ptr extends ptr<@c_const const_struct_pollfd_ptr> {}

    public static final c_short POLLIN = constant();
    public static final c_short POLLPRI = constant();
    public static final c_short POLLOUT = constant();
    public static final c_short POLLRDHUP = constant();
    public static final c_short POLLERR = constant();
    public static final c_short POLLHUP = constant();
    public static final c_short POLLNVAL = constant();

}
