package org.qbicc.runtime.posix;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.posix.SysTypes.*;
import static org.qbicc.runtime.stdc.Stddef.*;

@SuppressWarnings("SpellCheckingInspection")
@include("<sys/mman.h>")
@define(value = "_POSIX_C_SOURCE", as = "200809L")
public final class SysMman {
    private SysMman() {}

    public static native void_ptr mmap(void_ptr addr, size_t length, c_int prot, c_int flags, c_int fd, off_t offset);
    public static native c_int munmap(void_ptr addr, size_t length);

    public static native c_int mprotect(void_ptr addr, size_t length, c_int prot);

    public static native c_int mlock(const_void_ptr addr, size_t length);
    public static native c_int munlock(const_void_ptr addr, size_t length);

    public static native c_int mlockall(c_int flags);
    public static native c_int munlockall();

    public static native c_int msync(void_ptr addr, size_t length, c_int flags);

    public static native c_int posix_madvise(void_ptr addr, size_t length, c_int advice);

    public static native c_int posix_mem_offset(const_void_ptr addr, size_t length, off_t_ptr offsetPtr, size_t_ptr contigLen, int_ptr fdPtr);

    public static native c_int shm_open(const_char_ptr name, c_int oflag, mode_t mode);
    public static native c_int shm_unlink(const_char_ptr name);

    // NOTE: Not POSIX but widely supported
    public static final c_int MAP_ANON = constant();

    public static final c_int MAP_SHARED = constant();
    public static final c_int MAP_PRIVATE = constant();
    public static final c_int MAP_FIXED = constant();

    public static final void_ptr MAP_FAILED = constant();

    public static final c_int PROT_READ = constant();
    public static final c_int PROT_WRITE = constant();
    public static final c_int PROT_EXEC = constant();
    public static final c_int PROT_NONE = constant();

    public static final c_int POSIX_MADV_NORMAL = constant();
    public static final c_int POSIX_MADV_SEQUENTIAL = constant();
    public static final c_int POSIX_MADV_RANDOM = constant();
    public static final c_int POSIX_MADV_WILLNEED = constant();
    public static final c_int POSIX_MADV_DONTNEED = constant();
}
