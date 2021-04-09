package org.qbicc.runtime.posix;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.posix.SysTypes.*;
import static org.qbicc.runtime.posix.SysUio.*;
import static org.qbicc.runtime.stdc.Stddef.*;

/**
 *
 */
@define(value = "_POSIX_C_SOURCE", as = "200809L")
@include("<sys/socket.h>")
public final class SysSocket {
    public static final class socklen_t extends word {}

    public static final class socklen_t_ptr extends ptr<socklen_t> {}
    public static final class const_socklen_t_ptr extends ptr<@c_const socklen_t> {}
    public static final class socklen_t_ptr_ptr extends ptr<socklen_t_ptr> {}
    public static final class const_socklen_t_ptr_ptr extends ptr<const_socklen_t_ptr> {}
    public static final class socklen_t_ptr_const_ptr extends ptr<@c_const socklen_t_ptr> {}
    public static final class const_socklen_t_ptr_const_ptr extends ptr<@c_const const_socklen_t_ptr> {}

    public static final class sa_family_t extends word {}

    public static final class struct_sockaddr extends object {
        public sa_family_t sa_family;
        public c_char[] sa_data;
    }

    public static final class struct_sockaddr_ptr extends ptr<struct_sockaddr> {}
    public static final class const_struct_sockaddr_ptr extends ptr<@c_const struct_sockaddr> {}
    public static final class struct_sockaddr_ptr_ptr extends ptr<struct_sockaddr_ptr> {}
    public static final class const_struct_sockaddr_ptr_ptr extends ptr<const_struct_sockaddr_ptr> {}
    public static final class struct_sockaddr_ptr_const_ptr extends ptr<@c_const struct_sockaddr_ptr> {}
    public static final class const_struct_sockaddr_ptr_const_ptr extends ptr<@c_const const_struct_sockaddr_ptr> {}

    public static final class struct_sockaddr_storage extends object {
        public sa_family_t ss_family;
    }

    public static final class struct_msghdr extends object {
        public void_ptr msg_name;
        public socklen_t msg_namelen;
        public struct_iovec_ptr msg_iov;
        public c_int msg_iovlen;
        public void_ptr msg_control;
        public socklen_t msg_controllen;
        public c_int msg_flags;
    }

    public static final class struct_msghdr_ptr extends ptr<struct_msghdr> {}
    public static final class const_struct_msghdr_ptr extends ptr<@c_const struct_msghdr> {}
    public static final class struct_msghdr_ptr_ptr extends ptr<struct_msghdr_ptr> {}
    public static final class const_struct_msghdr_ptr_ptr extends ptr<const_struct_msghdr_ptr> {}
    public static final class struct_msghdr_ptr_const_ptr extends ptr<@c_const struct_msghdr_ptr> {}
    public static final class const_struct_msghdr_ptr_const_ptr extends ptr<@c_const const_struct_msghdr_ptr> {}

    public static final class struct_cmsghdr extends object {
        public socklen_t cmsg_len;
        public c_int cmsg_level;
        public c_int cmsg_type;
    }

    public static final c_int SCM_RIGHTS = constant();

    // todo: CMSG_*() macros

    public static final class struct_linger extends object {
        public c_int l_onoff;
        public c_int l_linger;
    }

    public static final c_int SOCK_DGRAM = constant();
    public static final c_int SOCK_RAW = constant();
    public static final c_int SOCK_SEQPACKET = constant();
    public static final c_int SOCK_STREAM = constant();

    public static final c_int SOL_SOCKET = constant();

    public static final c_int SO_ACCEPTCONN = constant();
    public static final c_int SO_BROADCAST = constant();
    public static final c_int SO_DEBUG = constant();
    public static final c_int SO_DONTROUTE = constant();
    public static final c_int SO_ERROR = constant();
    public static final c_int SO_KEEPALIVE = constant();
    public static final c_int SO_LINGER = constant();
    public static final c_int SO_OOBINLINE = constant();
    public static final c_int SO_RCVBUF = constant();
    public static final c_int SO_RCVLOWAT = constant();
    public static final c_int SO_RCVTIMEO = constant();
    public static final c_int SO_REUSEADDR = constant();
    public static final c_int SO_SNDBUF = constant();
    public static final c_int SO_SNDLOWAT = constant();
    public static final c_int SO_SNDTIMEO = constant();
    public static final c_int SO_TYPE = constant();

    public static final c_int SOMAXCONN = constant();

    public static final c_int MSG_CTRUNC = constant();
    public static final c_int MSG_DONTROUTE = constant();
    public static final c_int MSG_EOR = constant();
    public static final c_int MSG_OOB = constant();
    public static final c_int MSG_NOSIGNAL = constant();
    public static final c_int MSG_PEEK = constant();
    public static final c_int MSG_TRUNC = constant();
    public static final c_int MSG_WAITALL = constant();

    public static final c_int AF_INET = constant();
    public static final c_int AF_INET6 = constant();
    public static final c_int AF_UNIX = constant();
    public static final c_int AF_UNSPEC = zero();

    public static final c_int SHUT_RD = constant();
    public static final c_int SHUT_RDWR = constant();
    public static final c_int SHUT_WR = constant();

    public static native c_int accept(c_int fd, @restrict struct_sockaddr_ptr addr, @restrict socklen_t_ptr addrLen);
    @define("_GNU_SOURCE")
    public static native c_int accept4(c_int fd, @restrict struct_sockaddr_ptr addr, @restrict socklen_t_ptr addrLen, c_int flags);
    public static native c_int bind(c_int fd, const_struct_sockaddr_ptr addr, socklen_t addrLen);
    public static native c_int connect(c_int fd, const_struct_sockaddr_ptr addr, socklen_t addrLen);
    public static native c_int getpeername(c_int fd, @restrict struct_sockaddr_ptr addr, @restrict socklen_t_ptr addrLen);
    public static native c_int getsockname(c_int fd, @restrict struct_sockaddr_ptr addr, @restrict socklen_t_ptr addrLen);
    public static native c_int getsockopt(c_int fd, c_int level, c_int optName, @restrict void_ptr value, @restrict socklen_t_ptr optLen);
    public static native c_int listen(c_int fd, c_int backlog);
    public static native ssize_t recv(c_int fd, void_ptr buf, size_t len, c_int flags);
    public static native ssize_t recvfrom(c_int fd, @restrict void_ptr buf, size_t len, c_int flags, @restrict struct_sockaddr_ptr src_addr, @restrict socklen_t_ptr addrLen);
    public static native ssize_t recvmsg(c_int fd, struct_msghdr_ptr msg, c_int flags);
    public static native ssize_t send(c_int fd, const_void_ptr buf, size_t len, c_int flags);
    public static native ssize_t sendto(c_int fd, const_void_ptr buf, size_t len, c_int flags, const_struct_sockaddr_ptr dest_addr, socklen_t addrLen);
    public static native ssize_t sendmsg(c_int fd, const_struct_msghdr_ptr msg, c_int flags);
    public static native c_int setsockopt(c_int fd, c_int level, c_int optName, const_void_ptr value, socklen_t optLen);
    public static native c_int shutdown(c_int fd, c_int how);
    public static native c_int sockatmark(c_int fd);
    public static native c_int socket(c_int domain, c_int type, c_int protocol);
    public static native c_int socketpair(c_int domain, c_int type, c_int protocol, c_int @array_size(2)[] sv);

    @define("_GNU_SOURCE")
    public static final c_int SO_REUSEPORT = constant();
}
