package cc.quarkus.qcc.runtime.posix;

import static cc.quarkus.qcc.runtime.CNative.*;
import static cc.quarkus.qcc.runtime.posix.SysTypes.*;
import static cc.quarkus.qcc.runtime.stdc.Stddef.*;

import cc.quarkus.qcc.runtime.Build;

/**
 *
 */
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
    public static native c_int pipe(c_int @array_size(2) [] fds);

    // Alpha, IA-64, MIPS, SuperH, SPARC, SPARC64
    public static native struct_fd_pair pipe();

    public static native c_int unlink(ptr<@c_const c_char> pathname);

    public static native ssize_t write(c_int fd, ptr<@c_const ?> buf, size_t count);

    public static native ssize_t read(c_int fd, ptr<?> buf, size_t count);

    public static final c_int R_OK = constant();
    public static final c_int W_OK = constant();
    public static final c_int X_OK = constant();
    public static final c_int F_OK = constant();

    public static final c_int SEEK_SET = constant();
    public static final c_int SEEK_CUR = constant();
    public static final c_int SEEK_END = constant();

    public static native off_t lseek(c_int fd, off_t offset, c_int whence);

    public static native c_int fcntl(c_int fd, c_int cmd);

    public static native c_int fcntl(c_int fd, c_int cmd, object arg);
}
