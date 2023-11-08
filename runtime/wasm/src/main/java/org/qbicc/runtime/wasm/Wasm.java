package org.qbicc.runtime.wasm;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stdc.Stddef.*;
import static org.qbicc.runtime.stdc.Stdint.*;

/**
 * WASM run time support, including instruction mappings.
 */
public final class Wasm {
    private Wasm() {}

    // the initial data segments for the three address spaces

    public static final @addrSpace(258) ptr<?> initialGlobal = word(0);
    public static final @addrSpace(258) ptr<?> initialHeap = word(1);
    public static final @addrSpace(258) ptr<?> initialThreadLocal = word(128);

    // pointers which encode address space numbers for memories

    public static final @addrSpace(257) ptr<?> globalMem = word(0);
    public static final @addrSpace(257) ptr<?> heapMem = word(1);
    public static final @addrSpace(257) ptr<?> threadLocalMem = word(128);

    public static final class data {
        private data() {}

        /**
         * Inform the runtime that the data segment identified by {@code data} is no longer needed.
         *
         * @param data the (constant) data segment
         */
        public static native void drop(@addrSpace(258) ptr<?> data);
    }

    public static final class table {
        private table() {}
    }

    public static final class memory {
        private memory() {}

        public static final class atomic {
            private atomic() {}

            /**
             * Notify some number of waiters on the given aligned address.
             * The memory to access is determined by the address space of the given address.
             *
             * @param addr the address (must be aligned to 4 bytes)
             * @param waiterCnt the number of waiters to notify
             * @return the number of waiters that were awoken
             */
            public static native uint32_t notify(ptr<?> addr, uint32_t waiterCnt);

            /**
             * Wait for the value at the given address to change.
             * The memory to access is determined by the address space of the given address.
             *
             * @param addr the address (must be aligned to 4 bytes)
             * @param expected the expected value
             * @param timeout the maximum number of nanos to wait, or any negative number for unlimited waiting
             * @return 0 if awoken by another thread, 1 if the value at {@code addr} is not equal to {@code expected},
             *  or 2 if the timeout expired before the calling thread was awoken
             */
            public static native int wait32(ptr<?> addr, int expected, long timeout);

            /**
             * Wait for the value at the given address to change.
             * The memory to access is determined by the address space of the given address.
             *
             * @param addr the address (must be aligned to 8 bytes)
             * @param expected the expected value
             * @param timeout the maximum number of nanos to wait, or any negative number for unlimited waiting
             * @return 0 if awoken by another thread, 1 if the value at {@code addr} is not equal to {@code expected},
             *  or 2 if the timeout expired before the calling thread was awoken
             */
            public static native int wait64(ptr<?> addr, long expected, long timeout);
        }

        /**
         * Get the size of the given memory.
         *
         * @param addrSpace the (constant) address space
         * @return the current size of the memory (in 64KiB pages)
         */
        public static native uint32_t size(@addrSpace(257) ptr<?> addrSpace);

        /**
         * Fill memory with the given value.
         * The address space is obtained from the given pointer.
         *
         * @param dest the pointer to the region to fill
         * @param count the number of bytes to fill
         * @param value the value to store
         */
        public static native void fill(ptr<?> dest, size_t count, byte value);

        /**
         * Grow a memory by the given number of pages.
         *
         * @param addrSpace the (constant) address space
         * @param pages the number of 64KiB pages to grow the memory by
         * @return the number of pages on success, or {@code -1} on failure
         */
        public static native int grow(@addrSpace(257) ptr<?> addrSpace, int pages);

        /**
         * Copy bytes from one location to another, possibly between address spaces.
         * The source and destination address spaces are taken from the source and destination pointers.
         *
         * @param dest the destination address
         * @param src the source address
         * @param count the number of bytes to copy
         */
        public static native void copy(ptr<?> dest, ptr<?> src, size_t count);

        /**
         * Load bytes from a data segment into a memory.
         * The destination address space is obtained from the given pointer.
         *
         * @param data the (constant) data segment pointer
         * @param dest the destination address
         * @param srcOffs the offset within the data segment
         * @param count the number of bytes to load
         */
        public static native void init(@addrSpace(258) ptr<?> data, ptr<?> dest, uint32_t srcOffs, size_t count);
    }

    public static final class thread {
        private thread() {}

        /* todo
        public static native void spawn(ptr<function<thread_spawn_fn>> fnRef);
         */
    }
}
