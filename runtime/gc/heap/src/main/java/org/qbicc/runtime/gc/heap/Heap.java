package org.qbicc.runtime.gc.heap;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.posix.String.*;
import static org.qbicc.runtime.posix.SysMman.*;
import static org.qbicc.runtime.posix.SysMman.mprotect;
import static org.qbicc.runtime.posix.Unistd.*;
import static org.qbicc.runtime.stdc.Errno.*;
import static org.qbicc.runtime.stdc.Stdint.*;
import static org.qbicc.runtime.stdc.Stdio.*;
import static org.qbicc.runtime.stdc.Stdlib.*;
import static org.qbicc.runtime.stdc.String.*;

import java.lang.invoke.VarHandle;

import org.qbicc.runtime.Build;
import org.qbicc.runtime.Inline;

/**
 * Heap management utilities.
 * <p>
 * The heap index values follow this rule:
 * <p>
 * {@code 0 ≤ allocated ≤ Xms ≤ committed ≤ limit(Xmx)}
 */
public final class Heap {
    private Heap() {}

    private static boolean initOk;
    private static int initErrno;
    private static ptr<@c_const c_char> errorMsgTemplate;
    private static ptr<@c_const c_char> errorExtraArg;

    /**
     * The detected page size.
     */
    private static long pageSize;

    /**
     * The base pointer of the run time heap memory region.
     */
    private static void_ptr heap;
    /**
     * The allocated count for the heap, obeying this relation: {@code 0 ≤ allocated ≤ committed ≤ limit}.
     */
    private static long allocated;
    /**
     * The committed count for the heap, obeying this relation: {@code 0 ≤ allocated ≤ committed ≤ limit}.
     */
    private static long committed;
    /**
     * The upper limit for the heap, obeying this relation: {@code 0 ≤ allocated ≤ committed ≤ limit}.
     */
    private static long limit;

    /**
     * Get the build-time-configured maximum heap size.
     *
     * @return the configured maximum heap size in bytes
     */
    public static native long getConfiguredMinHeapSize();

    /**
     * Get the build-time-configured minimum heap size.
     *
     * @return the configured minimum heap size in bytes
     */
    public static native long getConfiguredMaxHeapSize();

    /**
     * Get the build-time-configured minimum heap alignment.
     *
     * @return the configured heap alignment
     */
    public static native long getConfiguredHeapAlignment();

    /**
     * Get the constant, build-time-configured minimum object alignment, in bytes.
     * It is always the case that {@code Integer.bitCount(getConfiguredObjectAlignment()) == 1}.
     *
     * @return the configured object alignment
     */
    public static native int getConfiguredObjectAlignment();

    /**
     * An OOME that can always be safely thrown without allocating anything on the heap.
     */
    public static final OutOfMemoryError OOME;

    static {
        //
        OutOfMemoryError error = new OutOfMemoryError();
        error.setStackTrace(new StackTraceElement[0]);
        OOME = error;
    }

    /**
     * Get a pointer to the given heap offset. No checking is performed on the value.
     *
     * @param offset the offset
     * @return the pointer (not {@code null})
     */
    public static void_ptr pointerToOffset(long offset) {
        return heap.plus(offset);
    }

    /**
     * Get the amount of free heap.  This does not include free space within allocated regions, which can only be
     * established by the garbage collector.
     *
     * @return the amount of unallocated space, in bytes
     */
    public static long getHeapUnallocated() {
        return limit - addr_of(allocated).loadSingleAcquire().longValue();
    }

    /**
     * Get the detected page size.
     *
     * @return the page size
     */
    public static long getPageSize() {
        return pageSize;
    }

    /**
     * Get the current heap offset.
     *
     * @return the heap offset
     */
    public static long getCurrentHeapOffset() {
        return Heap.allocated;
    }

    /**
     * Allocate space on the heap, throwing OOME if the allocation fails.  If the {@code expectedOffset} argument
     * is not {@code -1} and the returned value does not equal {@code expectedOffset}, nothing will be allocated
     * and the call must be repeated.
     *
     * @param expectedOffset the expected region offset, or {@code -1} if it does not matter
     * @param size the number of bytes to allocate (must be a multiple of the page size)
     * @return the previous heap region offset
     * @throws IllegalArgumentException if the size is not a valid allocation size
     * @throws OutOfMemoryError if some heap could not be committed
     */
    public static long allocateRegion(long expectedOffset, long size) throws IllegalArgumentException, OutOfMemoryError {
        if (Build.isHost()) {
            throw new IllegalStateException();
        }
        if ((size & (pageSize - 1)) != 0) {
            throw new IllegalArgumentException();
        }
        int64_t_ptr allocatedPtr = addr_of(Heap.allocated);
        long limit = Heap.limit;
        long oldOffset, newOffset;
        do {
            oldOffset = allocatedPtr.loadSingleAcquire().longValue();
            if (expectedOffset != -1L && oldOffset != expectedOffset) {
                // someone allocated a region
                return oldOffset;
            }
            newOffset = oldOffset + size;
            if (newOffset > limit) {
                // heap is fully allocated
                throw OOME;
            }
            // commit if needed
            commitUpTo(newOffset);
            // now try to update the allocated value
        } while (! allocatedPtr.compareAndSetRelease(word(oldOffset), word(newOffset)));
        // return the pointer to the new space
        return oldOffset;
    }

    public static void commitUpTo(long index) throws IllegalArgumentException, OutOfMemoryError {
        if (Build.isHost()) {
            throw new IllegalStateException();
        }
        if ((index & (pageSize - 1)) != 0) {
            throw new IllegalArgumentException();
        }
        if (index > limit) {
            // too much to commit
            throw OOME;
        }
        int64_t_ptr committedPtr = addr_of(Heap.committed);
        long committedOld;
        do {
            committedOld = committedPtr.loadSingleAcquire().longValue();
            if (committedOld >= index) {
                // OK
                return;
            }
            // need to commit it
            c_int res = mprotect(heap.plus(committedOld), word(index - committedOld), wordOr(PROT_READ, PROT_WRITE));
            if (res == word(-1)) {
                // failed to commit; throw without allocating anything
                throw OOME;
            }
        } while (! committedPtr.compareAndSetRelease(word(committedOld), word(index)));
        // requested memory is committed
        return;
    }

    /**
     * Check on the status of heap initialization and return a flag if the heap is usable.
     *
     * @param printErrors {@code true} to print errors to {@code stderr}, {@code false} to skip printing
     * @return {@code true} if the heap is usable, {@code false} otherwise
     */
    public static boolean checkInit(boolean printErrors) {
        if (! initOk) {
            if (printErrors) {
                int initErrno = Heap.initErrno;
                ptr<@c_const c_char> errorMsgTemplate = Heap.errorMsgTemplate;
                ptr<@c_const c_char> message;
                if (errorMsgTemplate.isZero()) {
                    message = utf8z("Heap initialization was not completed").cast();
                } else {
                    message = errorMsgTemplate;
                }
                final int bufSize = 256;
                ptr<c_char> buf = alloca(word(bufSize)).cast();
                ptr<c_char> arg;
                if (strerror_r(word(initErrno), buf, word(bufSize)).isNonZero()) {
                    arg = utf8z("(too long)").cast();
                } else {
                    arg = buf;
                }
                fprintf(stderr, message.cast(), arg, errorExtraArg);
                fflush(stderr);
            }
            return false;
        } else {
            return true;
        }
    }

    public static boolean isHeapArgument(ptr<@c_const c_char> argPtr) {
        return strncmp(argPtr.cast(), utf8z("-Xmx"), word(4)).isZero()
            || strncmp(argPtr.cast(), utf8z("-Xms"), word(4)).isZero()
            ;
    }

    @export
    public static boolean initHeap(int argc, char_ptr_ptr argv) {
        if (Build.isHost()) {
            throw new IllegalStateException();
        }
        // ↓ temporary ↓
        if (initOk) {
            // if ctor was used, don't initialize twice
            return true;
        }
        // ↑ temporary ↑
        long pageMask;
        // Initializer function - JDK AND HEAP NOT YET AVAILABLE
        if (Build.Target.isPosix()) {
            int pageSize = sysconf(_SC_PAGE_SIZE).intValue();
            if (pageSize == -1) {
                errorMsgTemplate = utf8z("Failed to determine page size: %s\n").cast();
                initErrno = errno;
                return false;
            }
            if (Integer.bitCount(pageSize) != 1) {
                // not a power of 2? but not likely enough to be worth spending memory on an error message
                return false;
            }
            Heap.pageSize = pageSize;
            pageMask = pageSize - 1;
        } else {
            errorMsgTemplate = utf8z("Platform not supported\n").cast();
            return false;
        }
        // cycle through the arguments to find heap size options
        long maxHeap = -1;
        long minHeap = -1;
        for (int i = 1; i < argc; i ++) {
            const_char_ptr arg = argv.asArray()[i].cast();
            if (strncmp(arg, utf8z("-Xmx"), word(4)) == zero()) {
                maxHeap = parseMemSize(arg.plus(4));
                if (maxHeap == -1) {
                    // failed
                    return false;
                }
                if (maxHeap < pageSize) {
                    maxHeap = pageSize;
                }
            } else if (strncmp(arg, utf8z("-Xms"), word(4)) == zero()) {
                minHeap = parseMemSize(arg.plus(4));
                if (minHeap == -1) {
                    // failed
                    return false;
                }
                if (minHeap < pageSize) {
                    minHeap = pageSize;
                }
            } else if (strcmp(arg, utf8z("--")) == zero()) {
                // arguments done
                break;
            }
        }
        if (maxHeap == -1 && minHeap == -1) {
            maxHeap = getConfiguredMaxHeapSize();
            minHeap = getConfiguredMinHeapSize();
            // round up to page size
            maxHeap = (maxHeap + pageMask) & ~pageMask;
            minHeap = (minHeap + pageMask) & ~pageMask;
            if (maxHeap < minHeap) {
                maxHeap = minHeap;
            }
        } else if (maxHeap == -1) {
            maxHeap = getConfiguredMaxHeapSize();
            // round up to page size
            maxHeap = (maxHeap + pageMask) & ~pageMask;
            minHeap = (minHeap + pageMask) & ~pageMask;
            if (maxHeap < minHeap) {
                // minHeap takes precedence because maxHeap was not given
                maxHeap = minHeap;
            }
        } else if (minHeap == -1) {
            minHeap = getConfiguredMinHeapSize();
            // round up to page size
            maxHeap = (maxHeap + pageMask) & ~pageMask;
            minHeap = (minHeap + pageMask) & ~pageMask;
            if (maxHeap < minHeap) {
                // maxHeap takes precedence because minHeap was not given
                minHeap = maxHeap;
            }
        } else {
            // round up to page size
            maxHeap = (maxHeap + pageMask) & ~pageMask;
            minHeap = (minHeap + pageMask) & ~pageMask;
            if (maxHeap < minHeap) {
                maxHeap = minHeap;
            }
        }
        // First, reserve the full address space that we need
        long heapAlignment = getConfiguredHeapAlignment();
        void_ptr heap = mmap(zero(),
            word(maxHeap + heapAlignment),
            PROT_NONE,
            wordOr(MAP_ANON, MAP_PRIVATE),
            word(-1),
            zero()
        );
        if (heap == MAP_FAILED) {
            errorMsgTemplate = utf8z("Failed to map initial heap: %s\n").cast();
            initErrno = errno;
            // failed
            return false;
        }
        // align the heap
        long misalignment = heap.longValue() & (heapAlignment - 1);
        // release the extra address space at the start and the end
        long trimAtStart = heapAlignment - misalignment;
        if (trimAtStart > 0) {
            munmap(heap, word(trimAtStart));
            heap = heap.plus(trimAtStart);
        }
        // (heap is now aligned)
        if (misalignment > 0) {
            munmap(heap.plus(maxHeap), word(misalignment));
        }
        // next attempt to commit the minimum heap size
        c_int res = mprotect0(minHeap, heap);
        if (res == word(-1)) {
            errorMsgTemplate = utf8z("Failed to commit minimum heap space: %s\n").cast();
            initErrno = errno;
            // failed
            return false;
        }
        Heap.heap = heap;
        Heap.allocated = 0;
        Heap.committed = minHeap;
        Heap.limit = maxHeap;
        Heap.initOk = true;
        VarHandle.releaseFence();
        // The empty heap is configured!
        return true;
    }

    @Inline
    private static c_int mprotect0(long minHeap, void_ptr heap) {
        if (Build.Target.isWasm()) {
            return word(0);
        } else {
            return mprotect(heap, word(minHeap), wordOr(PROT_READ, PROT_WRITE));
        }
    }

    @export(withScope = ExportScope.LOCAL)
    private static long parseMemSize(final char_ptr arg) {
        char_ptr endPtr = auto();
        long num = strtoll(arg.cast(), addr_of(endPtr), word(10)).longValue();
        char ch = endPtr.loadUnshared().cast(unsigned_char.class).charValue();
        if (ch == 0) {
            // just bytes
        } else if (ch == 'T' || ch == 't') {
            // terabytes
            num *= 1L << 40;
            endPtr = endPtr.plus(1);
        } else if (ch == 'G' || ch == 'g') {
            // gigabytes
            num *= 1L << 30;
            endPtr = endPtr.plus(1);
        } else if (ch == 'M' || ch == 'm') {
            // megabytes
            num *= 1L << 20;
            endPtr = endPtr.plus(1);
        } else if (ch == 'K' || ch == 'k') {
            // kilobytes
            num *= 1L << 10;
            endPtr = endPtr.plus(1);
        }
        ch = endPtr.loadUnshared().cast(unsigned_char.class).charValue();
        if (ch != 0) {
            errorMsgTemplate = utf8z("Invalid memory size: %2$s\n").cast();
            errorExtraArg = arg;
            return -1;
        }
        return num;
    }

    @destructor(priority = 0)
    @export
    static void destroyHeap() {
        VarHandle.acquireFence();
        void_ptr heap = Heap.heap;
        if (heap != null) {
            munmap(heap, word(limit));
        }
        Heap.heap = zero();
        VarHandle.releaseFence();
    }
}
