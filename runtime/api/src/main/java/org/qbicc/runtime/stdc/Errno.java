package org.qbicc.runtime.stdc;

import static org.qbicc.runtime.CNative.*;

import org.qbicc.runtime.patcher.AccessWith;
import org.qbicc.runtime.patcher.Accessor;
import org.qbicc.runtime.Build;

/**
 *
 */
@include("<errno.h>")
@define(value = "_THREAD_SAFE_ERRNO", as = "1", when = Build.Target.IsAix.class) // TODO this should be global for AIX
public final class Errno {
    @AccessWith(value = GLibCErrnoAccessor.class, when = Build.Target.IsGLibCLike.class)
    @AccessWith(value = MacOsErrnoAccessor.class, when = Build.Target.IsMacOs.class)
    @AccessWith(value = AixErrnoAccessor.class, when = Build.Target.IsAix.class)
    public static int errno;

    // typedef of c_int
    public static final class errno_t extends word {
    }

    public static final class GLibCErrnoAccessor implements Accessor<Integer> {
        private static native int_ptr __errno_location();

        public int getAsInt() {
            return __errno_location().loadUnshared().intValue();
        }

        public void set(int value) {
            __errno_location().storeUnshared(word(value));
        }
    }

    public static final class MacOsErrnoAccessor implements Accessor<Integer> {
        private static native int_ptr __error();

        public int getAsInt() {
            return __error().loadUnshared().intValue();
        }

        public void set(int value) {
            __error().storeUnshared(word(value));
        }
    }

    public static final class AixErrnoAccessor implements Accessor<Integer> {
        private static native int_ptr _Errno();

        public int getAsInt() {
            return _Errno().loadUnshared().intValue();
        }

        public void set(int value) {
            _Errno().storeUnshared(word(value));
        }
    }

    public static final c_int EDOM = constant();
    public static final c_int EILSEQ = constant();
    public static final c_int ERANGE = constant();
}
