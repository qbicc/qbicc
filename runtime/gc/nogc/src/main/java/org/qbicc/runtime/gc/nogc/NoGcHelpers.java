package org.qbicc.runtime.gc.nogc;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stdc.Stdint.*;
import static org.qbicc.runtime.stdc.String.*;

import org.qbicc.runtime.AutoQueued;
import org.qbicc.runtime.Hidden;
import org.qbicc.runtime.NoSafePoint;
import org.qbicc.runtime.gc.heap.Heap;

/**
 *
 */
public final class NoGcHelpers {
    private NoGcHelpers() {}

    // our allocation position
    private static long pos;

    @Hidden
    @AutoQueued
    @NoSafePoint
    public static Object allocate(long size, int align) {
        // todo: per-object alignment - should we allow it? perhaps not (ignore for now)
        int64_t_ptr posPtr = addr_of(pos);

        long oldPos, newPos;
        long oldRegionPos;
        for (;;) {
            oldPos = posPtr.loadSingleAcquire().longValue();
            oldRegionPos = Heap.getCurrentHeapOffset();
            if (oldPos + size >= oldRegionPos) {
                // allocate some more pages
                long pageSize = Heap.getPageSize();
                long pageMask = pageSize - 1;
                // this is how far over we are (in pages)
                long amount = size - oldRegionPos + oldPos + pageMask & ~pageMask;
                if (Heap.allocateRegion(oldRegionPos, amount) != oldRegionPos) {
                    // our requested heap was not allocated; retry the loop
                    continue;
                }
            }
            newPos = oldPos + size;
            int objAlign = Heap.getConfiguredObjectAlignment();
            int objAlignMask = objAlign - 1;
            long misalign = newPos & objAlignMask;
            if (misalign != 0) {
                newPos += objAlign - misalign;
            }
            if (posPtr.compareAndSetRelease(word(oldPos), word(newPos))) {
                return ptrToRef(Heap.pointerToOffset(oldPos));
            }
            Thread.onSpinWait();
        }
    }

    @Hidden
    @AutoQueued
    @NoSafePoint
    public static void clear(Object ptr, long size) { memset(refToPtr(ptr), word(0), word(size)); }

    @Hidden
    @AutoQueued
    @NoSafePoint
    public static void copy(Object to, Object from, long size) {
        memcpy(refToPtr(to), refToPtr(from), word(size));
    }
}
