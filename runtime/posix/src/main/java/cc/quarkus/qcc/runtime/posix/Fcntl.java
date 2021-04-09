package org.qbicc.runtime.posix;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.posix.SysTypes.*;

/**
 *
 */
@define(value = "_POSIX_C_SOURCE", as = "200809L")
@include("<fcntl.h>")
public class Fcntl {
    public static native c_int open(const_char_ptr pathname, c_int flags);

    public static native c_int open(const_char_ptr pathname, c_int flags, mode_t mode);

    public static native c_int openat(c_int dirFd, const_char_ptr pathname, c_int flags);

    public static native c_int openat(c_int dirFd, const_char_ptr pathname, c_int flags, mode_t mode);

    public static final c_int O_CREAT = constant();
    public static final c_int O_EXCL = constant();
    public static final c_int O_NOCTTY = constant();
    public static final c_int O_TRUNC = constant();

    public static final c_int O_APPEND = constant();
    public static final c_int O_DSYNC = constant();
    public static final c_int O_NONBLOCK = constant();
    public static final c_int O_RSYNC = constant();
    public static final c_int O_SYNC = constant();

    public static final c_int O_ACCMODE = constant();

    public static final c_int O_RDONLY = constant();
    public static final c_int O_RDWR = constant();
    public static final c_int O_WRONLY = constant();


    public static final c_int O_FSYNC = constant();
    public static final c_int O_ASYNC = constant();
    public static final c_int O_DIRECTORY = constant();
    public static final c_int O_NOFOLLOW = constant();
    public static final c_int O_CLOEXEC = constant();
    public static final c_int O_DIRECT = constant();

    public static final c_int FD_CLOEXEC = constant();

    public static final c_int F_DUPFD = constant();
    public static final c_int F_GETFD = constant();
    public static final c_int F_SETFD = constant();
    public static final c_int F_GETFL = constant();
    public static final c_int F_SETFL = constant();
    public static final c_int F_GETLK = constant();
    public static final c_int F_SETLK = constant();
    public static final c_int F_GETOWN = constant();
    public static final c_int F_SETOWN = constant();

    public static final c_int F_RDLCK = constant();
    public static final c_int F_UNLCK = constant();
    public static final c_int F_WRLCK = constant();

    public static final c_int SEEK_SET = Unistd.SEEK_SET;
    public static final c_int SEEK_CUR = Unistd.SEEK_CUR;
    public static final c_int SEEK_END = Unistd.SEEK_END;

    public static final c_int AT_FDCWD = constant();

    public static final c_int AT_EACCESS = constant();

    public static final c_int AT_SYMLINK_NOFOLLOW = constant();

    public static final c_int AT_SYMLINK_FOLLOW = constant();

    public static final c_int AT_REMOVEDIR = constant();
}
