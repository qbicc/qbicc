package cc.quarkus.c_native.stdc;

import static cc.quarkus.c_native.api.CNative.*;

import cc.quarkus.qcc.plugin.patcher.AccessWith;
import cc.quarkus.qcc.plugin.patcher.Accessor;
import cc.quarkus.c_native.api.Build;

/**
 *
 */
@include("<errno.h>")
@define(value = "_THREAD_SAFE_ERRNO", as = "1", when = Build.Target.IsAix.class) // TODO this should be global for AIX
public final class Errno {
    @AccessWith(value = GLibCErrnoAccessor.class, when = Build.Target.IsGLibCLike.class)
    @AccessWith(value = MacOsErrnoAccessor.class, when = Build.Target.IsMacOs.class)
    @AccessWith(value = AixErrnoAccessor.class, when = Build.Target.IsAix.class)
    public static c_int errno;

    // typedef of c_int
    public static final class errno_t extends word {
    }

    public static final class GLibCErrnoAccessor implements Accessor<c_int> {
        private static native ptr<c_int> __errno_location();

        public c_int get() {
            return __errno_location().deref();
        }

        public void set(c_int value) {
            __errno_location().derefAssign(value);
        }
    }

    public static final class MacOsErrnoAccessor implements Accessor<c_int> {
        private static native ptr<c_int> __error();

        public c_int get() {
            return __error().deref();
        }

        public void set(c_int value) {
            __error().derefAssign(value);
        }
    }

    public static final class AixErrnoAccessor implements Accessor<c_int> {
        private static native ptr<c_int> _Errno();

        public c_int get() {
            return _Errno().deref();
        }

        public void set(c_int value) {
            _Errno().derefAssign(value);
        }
    }

    public static final c_int EDOM = constant();
    public static final c_int EILSEQ = constant();
    public static final c_int ERANGE = constant();
}
