package cc.quarkus.qcc.runtime.posix;

import static cc.quarkus.qcc.runtime.CNative.*;

/**
 *
 * @see cc.quarkus.qcc.runtime.stdc.Errno
 */
@define(value = "_POSIX_C_SOURCE", as = "200809L")
@include("<errno.h>")
public final class Errno {

    public static final c_int E2BIG = constant();
    public static final c_int EACCES = constant();
    public static final c_int EADDRINUSE = constant();
    public static final c_int EADDRNOTAVAIL = constant();
    public static final c_int EAFNOSUPPORT = constant();
    public static final c_int EAGAIN = constant();
    public static final c_int EALREADY = constant();
    public static final c_int EBADF = constant();
    public static final c_int EBADMSG = constant();
    public static final c_int EBUSY = constant();
    public static final c_int ECANCELED = constant();
    public static final c_int ECHILD = constant();
    public static final c_int ECONNABORTED = constant();
    public static final c_int ECONNREFUSED = constant();
    public static final c_int EDEADLK = constant();
    public static final c_int EDESTADDRREQ = constant();
    // EDOM is stdc
    public static final c_int EDQUOT = constant();
    public static final c_int EEXIST = constant();
    public static final c_int EFAULT = constant();
    public static final c_int EFBIG = constant();
    public static final c_int EHOSTUNREACH = constant();
    public static final c_int EIDRM = constant();
    // EILSEQ is stdc
    public static final c_int EINPROGRESS = constant();
    public static final c_int EINTR = constant();
    public static final c_int EINVAL = constant();
    public static final c_int EIO = constant();
    public static final c_int EISCONN = constant();
    public static final c_int EISDIR = constant();
    public static final c_int ELOOP = constant();
    public static final c_int EMFILE = constant();
    public static final c_int EMLINK = constant();
    public static final c_int EMSGSIZE = constant();
    public static final c_int EMULTIHOP = constant();
    public static final c_int ENAMETOOLONG = constant();
    public static final c_int ENETDOWN = constant();
    public static final c_int ENETRESET = constant();
    public static final c_int ENETUNREACH = constant();
    public static final c_int ENFILE = constant();
    public static final c_int ENOBUFS = constant();
    public static final c_int ENODATA = constant();
    public static final c_int ENODEV = constant();
    public static final c_int ENOENT = constant();
    public static final c_int ENOEXEC = constant();
    public static final c_int ENOLCK = constant();
    // ENOLINK reserved
    public static final c_int ENOMEM = constant();
    public static final c_int ENOMSG = constant();
    public static final c_int ENOPROTOOPT = constant();
    public static final c_int ENOSPC = constant();
    public static final c_int ENOSR = constant();
    public static final c_int ENOSTR = constant();
    public static final c_int ENOSYS = constant();
    public static final c_int ENOTCONN = constant();
    public static final c_int ENOTDIR = constant();
    public static final c_int ENOTEMPTY = constant();
    public static final c_int ENOTRECOVERABLE = constant();
    public static final c_int ENOTSOCK = constant();
    public static final c_int ENOTSUP = constant();
    public static final c_int ENOTTY = constant();
    public static final c_int ENXIO = constant();
    public static final c_int EOPNOTSUPP = constant();
    public static final c_int EOVERFLOW = constant();
    public static final c_int EOWNERDEAD = constant();
    public static final c_int EPERM = constant();
    public static final c_int EPIPE = constant();
    public static final c_int EPROTO = constant();
    public static final c_int EPROTONOTSUPPORT = constant();
    public static final c_int EPROTOTYPE = constant();
    // ERANGE is stdc
    public static final c_int EROFS = constant();
    public static final c_int ESPIPE = constant();
    public static final c_int ESRCH = constant();
    // ESTALE reserved
    public static final c_int ETIME = constant();
    public static final c_int ETIMEDOUT = constant();
    public static final c_int ETXTBSY = constant();
    public static final c_int EWOULDBLOCK = constant();
    public static final c_int EXDEV = constant();

}
