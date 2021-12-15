package org.qbicc.runtime.posix;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.posix.SysTypes.*;
import static org.qbicc.runtime.stdc.Stddef.*;

import org.qbicc.runtime.Build;

/**
 *
 */
@SuppressWarnings("SpellCheckingInspection")
@include(value = "<unistd.h>", when = Build.Target.IsUnix.class)
public final class Unistd {

    private Unistd() {
        /* empty */ }

    @incomplete
    public static final class struct_fd_pair extends object {
        public c_long @array_size(2) [] fd;
    }

    public static native c_int close(c_int fd);

    public static native c_int dup(c_int fd);

    public static native c_int dup2(c_int fd1, c_int fd2);

    @define(value = "_POSIX_C_SOURCE", as = "200112L", when = Build.Target.IsPosix.class)
    public static native c_int fsync(c_int fd);

    @define(value = "_POSIX_C_SOURCE", as = "199309L", when = Build.Target.IsPosix.class)
    public static native c_int fdatasync(c_int fd);

    public static native pid_t fork();

    // POSIX
    public static native c_int pipe(c_int[] fds);

    // Alpha, IA-64, MIPS, SuperH, SPARC, SPARC64
    public static native struct_fd_pair pipe();

    public static native c_int unlink(const_char_ptr pathname);

    public static native ssize_t write(c_int fd, const_void_ptr buf, size_t count);

    public static native ssize_t read(c_int fd, void_ptr buf, size_t count);

    public static final c_int R_OK = constant();
    public static final c_int W_OK = constant();
    public static final c_int X_OK = constant();
    public static final c_int F_OK = constant();

    public static final c_int SEEK_SET = constant();
    public static final c_int SEEK_CUR = constant();
    public static final c_int SEEK_END = constant();

    public static native off_t lseek(c_int fd, off_t offset, c_int whence);

    public static native c_int fcntl(c_int fd, c_int cmd);

    public static native c_int fcntl(c_int fd, c_int cmd, object... arg);

    public static final c_int _SC_ARG_MAX = constant();
    public static final c_int _SC_CHILD_MAX = constant();
    public static final c_int _SC_HOST_NAME_MAX = constant();
    public static final c_int _SC_LOGIN_NAME_MAX = constant();
    public static final c_int _SC_NGROUPS_MAX = constant();
    public static final c_int _SC_OPEN_MAX = constant();
    public static final c_int _SC_PAGE_SIZE = constant();
    public static final c_int _SC_SYMLOOP_MAX = constant();
    public static final c_int _SC_TTY_NAME_MAX = constant();
    public static final c_int _SC_TZNAME_MAX = constant();

    public static native c_long sysconf(c_int name);

    public static native pid_t getpid();
    public static native pid_t getppid();
}
