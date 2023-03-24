package org.qbicc.runtime.gc.heap;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stdc.Stddef.*;

import org.qbicc.runtime.Build;

@SuppressWarnings("SpellCheckingInspection")
class SysHeaders {
    @include("<string.h>")
    @define(value = "_POSIX_C_SOURCE", as = "200809L")
    static final class String {
        @name(value = "__xpg_strerror_r", when = Build.Target.IsGLibCLike.class)
        public static native c_int strerror_r(c_int errno, ptr<c_char> buf, size_t bufLen);

        // GLIBC only
        @Deprecated
        public static native c_int __xpg_strerror_r(c_int errno, ptr<c_char> buf, size_t bufLen);
    }

    @include(value = "<sys/types.h>", when = Build.Target.IsPosix.class)
     static final class SysTypes {
        public static final class off_t extends word {
        }
    }

    @include("<sys/mman.h>")
    @define(value = "_POSIX_C_SOURCE", as = "200809L")
    @define(value = "_DARWIN_C_SOURCE", when = Build.Target.IsApple.class)
    static final class SysMman {
        public static native c_int mprotect(void_ptr addr, size_t length, c_int prot);

        public static native void_ptr mmap(void_ptr addr, size_t length, c_int prot, c_int flags, c_int fd, SysTypes.off_t offset);
        public static native c_int munmap(void_ptr addr, size_t length);

        public static final c_int MAP_ANON = constant();  // NOTE: Not POSIX but widely supported
        public static final c_int MAP_PRIVATE = constant();

        public static final void_ptr MAP_FAILED = constant();
        public static final c_int PROT_READ = constant();
        public static final c_int PROT_WRITE = constant();
        public static final c_int PROT_NONE = constant();
    }

    @include(value = "<unistd.h>", when = Build.Target.IsUnix.class)
    static final class Unistd {
        public static native c_long sysconf(c_int name);
        public static final c_int _SC_PAGE_SIZE = constant();
    }
}
