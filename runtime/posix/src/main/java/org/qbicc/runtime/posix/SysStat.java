package org.qbicc.runtime.posix;

import org.qbicc.runtime.Build;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.posix.SysTypes.*;
import static org.qbicc.runtime.stdc.Time.*;

/**
 *
 */
@define(value = "_POSIX_C_SOURCE", as = "200809L")
@include("<sys/stat.h>")
public final class SysStat {

    public static final class struct_stat extends object {
        public dev_t st_dev;
        public ino_t st_ino;
        public mode_t st_mode;
        public nlink_t st_nlink;
        public uid_t st_uid;
        public gid_t st_gid;
        public dev_t st_rdev;
        public off_t st_size;
        public time_t st_atime;
        public time_t st_mtime;
        public time_t st_ctime;
        public blksize_t st_blksize;
        public blkcnt_t st_blocks;
    }

    public static final class struct_stat_ptr extends ptr<struct_stat> {}

    public static final class struct_stat64 extends object {
        public dev_t st_dev;
        public ino64_t st_ino;
        public mode_t st_mode;
        public nlink_t st_nlink;
        public uid_t st_uid;
        public gid_t st_gid;
        public dev_t st_rdev;
        public off64_t st_size;
        public time_t st_atime;
        public time_t st_mtime;
        public time_t st_ctime;
        public blksize_t st_blksize;
        public blkcnt64_t st_blocks;
    }

    public static final class struct_stat64_ptr extends ptr<struct_stat64> {}

    public static final mode_t S_IFMT = constant();
    public static final mode_t S_IFBLK = constant();
    public static final mode_t S_IFCHR = constant();
    public static final mode_t S_IFIFO = constant();
    public static final mode_t S_IFREG = constant();
    public static final mode_t S_IFDIR = constant();
    public static final mode_t S_IFLNK = constant();
    public static final mode_t S_IFSOCK = constant();

    public static final mode_t S_IRWXU = constant();
    public static final mode_t S_IRUSR = constant();
    public static final mode_t S_IWUSR = constant();
    public static final mode_t S_IXUSR = constant();

    public static final mode_t S_IRWXG = constant();
    public static final mode_t S_IRGRP = constant();
    public static final mode_t S_IWGRP = constant();
    public static final mode_t S_IXGRP = constant();

    public static final mode_t S_IRWXO = constant();
    public static final mode_t S_IROTH = constant();
    public static final mode_t S_IWOTH = constant();
    public static final mode_t S_IXOTH = constant();

    @name(value = "stat$INODE64", when = { Build.Target.IsMacOs.class, Build.Target.IsAmd64.class })
    public static native c_int stat(const_char_ptr pathName, struct_stat_ptr statBuf);
    @name(value = "fstat$INODE64", when = { Build.Target.IsMacOs.class, Build.Target.IsAmd64.class })
    public static native c_int fstat(c_int fd, struct_stat_ptr statBuf);
    @name(value = "lstat$INODE64", when = { Build.Target.IsMacOs.class, Build.Target.IsAmd64.class })
    public static native c_int lstat(const_char_ptr pathName, struct_stat_ptr statBuf);
    @name(value = "fstatat$INODE64", when = { Build.Target.IsMacOs.class, Build.Target.IsAmd64.class })
    public static native c_int fstatat(c_int dirFd, const_char_ptr pathName, struct_stat_ptr statBuf, c_int flags);
}
