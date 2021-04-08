package cc.quarkus.qcc.runtime.gc.nogc;

import static cc.quarkus.qcc.runtime.CNative.*;
import static cc.quarkus.qcc.runtime.posix.Stdlib.*;
import static cc.quarkus.qcc.runtime.stdc.Stddef.*;
import static cc.quarkus.qcc.runtime.stdc.Stdlib.*;
import static cc.quarkus.qcc.runtime.stdc.String.*;

import cc.quarkus.qcc.runtime.Build;

/**
 *
 */
public final class NoGcHelpers {
    private NoGcHelpers() {}

    public static ptr<?> allocate(long size, int align) {
        if (false && Build.Target.isPosix()) {
            void_ptr ptr = auto();
            c_int res = posix_memalign(addr_of(ptr), word((long)align), word(size));
            if (res.intValue() != 0) {
                // todo: read errno
                throw new OutOfMemoryError(/*"Allocation failed"*/);
            }
            return ptr;
        } else {
            char_ptr ptr = malloc(word(size + align));
            if (ptr.isNull()) {
                throw new OutOfMemoryError(/*"Allocation failed"*/);
            }
            long mask = align - 1;
            long misAlign = ptr.longValue() & mask;
            if (misAlign != 0) {
                ptrdiff_t word = word(((~ misAlign) & mask) + 1);
                ptr = ptr.plus(word);
            }
            return ptr;
        }
    }

    public static void_ptr clear(void_ptr ptr, long size) { return memset(ptr, word(0), word(size)); }

    public static void copy(void_ptr to, const_void_ptr from, long size) {
        memcpy(to, from, word(size));
    }
}
