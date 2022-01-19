package org.qbicc.runtime.gc.nogc;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.posix.Stdlib.*;
import static org.qbicc.runtime.stdc.Stddef.*;
import static org.qbicc.runtime.stdc.Stdlib.*;
import static org.qbicc.runtime.stdc.String.*;

import org.qbicc.runtime.AutoQueued;
import org.qbicc.runtime.Build;
import org.qbicc.runtime.Hidden;

/**
 *
 */
public final class NoGcHelpers {
    private NoGcHelpers() {}

    @Hidden
    @AutoQueued
    public static Object allocate(long size, int align) {
        if (false && Build.Target.isPosix()) {
            void_ptr ptr = auto();
            c_int res = posix_memalign(addr_of(ptr), word((long)align), word(size));
            if (res.intValue() != 0) {
                // todo: read errno
                throw new OutOfMemoryError(/*"Allocation failed"*/);
            }
            return ptr;
        } else {
            void_ptr ptr = malloc(word(size + align));
            if (ptr.isNull()) {
                throw new OutOfMemoryError(/*"Allocation failed"*/);
            }
            long mask = align - 1;
            long misAlign = ptr.longValue() & mask;
            if (misAlign != 0) {
                ptrdiff_t word = word(((~ misAlign) & mask) + 1);
                ptr = ptr.plus(word.intValue());
            }
            return ptrToRef(ptr);
        }
    }

    @Hidden
    @AutoQueued
    public static void clear(Object ptr, long size) { memset((void_ptr)(ptr<?>)refToPtr(ptr), word(0), word(size)); }

    @Hidden
    @AutoQueued
    public static void copy(Object to, Object from, long size) {
        memcpy((void_ptr)(ptr<?>)refToPtr(to), (const_void_ptr)(ptr<?>)refToPtr(from), word(size));
    }
}
