package org.qbicc.runtime.stdc;

import static org.qbicc.runtime.CNative.*;

import org.qbicc.runtime.NoThrow;
import org.qbicc.runtime.SafePoint;
import org.qbicc.runtime.SafePointBehavior;
import org.qbicc.runtime.patcher.AccessWith;
import org.qbicc.runtime.patcher.Accessor;
import org.qbicc.runtime.Build;

/**
 *
 */
@include("<errno.h>")
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
    @SafePoint(SafePointBehavior.ALLOWED)
    private static native ptr<c_int> __errno_location();

    @SafePoint(SafePointBehavior.ALLOWED)
    @NoThrow
    public int getAsInt() {
        return __errno_location().loadUnshared().intValue();
    }

    @SafePoint(SafePointBehavior.ALLOWED)
    @NoThrow
    public void set(int value) {
        __errno_location().storeUnshared(word(value));
    }
}

final class MacOsErrnoAccessor implements Accessor<Integer> {
    @extern
    @SafePoint(SafePointBehavior.ALLOWED)
    private static native ptr<c_int> __error();

    @SafePoint(SafePointBehavior.ALLOWED)
    @NoThrow
    public int getAsInt() {
        return __error().loadUnshared().intValue();
    }

    @SafePoint(SafePointBehavior.ALLOWED)
    @NoThrow
    public void set(int value) {
        __error().storeUnshared(word(value));
    }
}

final class AixErrnoAccessor implements Accessor<Integer> {
    @extern
    @SafePoint(SafePointBehavior.ALLOWED)
    private static native ptr<c_int> _Errno();

    @SafePoint(SafePointBehavior.ALLOWED)
    @NoThrow
    public int getAsInt() {
        return _Errno().loadUnshared().intValue();
    }

    @SafePoint(SafePointBehavior.ALLOWED)
    @NoThrow
    public void set(int value) {
        _Errno().storeUnshared(word(value));
    }
}
