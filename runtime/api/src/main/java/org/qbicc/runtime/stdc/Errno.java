package org.qbicc.runtime.stdc;

import static org.qbicc.runtime.CNative.*;

import org.qbicc.runtime.NoSafePoint;
import org.qbicc.runtime.NoThrow;
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

    public static final c_int EDOM = constant();
    public static final c_int EILSEQ = constant();
    public static final c_int ERANGE = constant();
}

// Although it would be nicer, defining these as nested classes of Errno
// doesn't work in qbicc yet because DefinedTypeDefinitionImpl.load()
// doesn't guard against the same thread re-entering load recursively
// which happens when the AccessorTypeBuilder tries to load these classes,
// which in turn wants to load their nestHost.

final class GLibCErrnoAccessor implements Accessor<Integer> {
    @extern
    private static native ptr<c_int> __errno_location();

    @NoSafePoint
    @NoThrow
    public int getAsInt() {
        return __errno_location().loadUnshared().intValue();
    }

    @NoSafePoint
    @NoThrow
    public void set(int value) {
        __errno_location().storeUnshared(word(value));
    }
}

final class MacOsErrnoAccessor implements Accessor<Integer> {
    @extern
    private static native ptr<c_int> __error();

    @NoSafePoint
    @NoThrow
    public int getAsInt() {
        return __error().loadUnshared().intValue();
    }

    @NoSafePoint
    @NoThrow
    public void set(int value) {
        __error().storeUnshared(word(value));
    }
}

final class AixErrnoAccessor implements Accessor<Integer> {
    @extern
    private static native ptr<c_int> _Errno();

    @NoSafePoint
    @NoThrow
    public int getAsInt() {
        return _Errno().loadUnshared().intValue();
    }

    @NoSafePoint
    @NoThrow
    public void set(int value) {
        _Errno().storeUnshared(word(value));
    }
}
