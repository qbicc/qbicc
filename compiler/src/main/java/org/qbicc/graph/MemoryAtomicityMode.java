package org.qbicc.graph;

import static org.qbicc.graph.atomic.AccessModes.*;

import org.qbicc.graph.atomic.AccessMode;
import org.qbicc.graph.atomic.AccessModes;

/**
 * The mode for a memory access operation.
 *
 * @deprecated Use the modes in {@link AccessModes} instead.
 */
@Deprecated
public enum MemoryAtomicityMode {
    /**
     * Non-atomic access (cannot be given for fence or read-modify-write operations).
     */
    NONE(SingleUnshared),
    /**
     * Plain access (cannot be given for fence or read-modify-write operations); any value that is read was written by
     * some other operation.
     */
    UNORDERED(SinglePlain),
    /**
     * Single total order for a given memory address (cannot be given for fence operations).
     */
    MONOTONIC(SingleOpaque),
    /**
     * Like {@link #MONOTONIC} but also forms a synchronization edge with a corresponding {@link #RELEASE} operation.
     */
    ACQUIRE(SingleAcquire),
    /**
     * Like {@link #MONOTONIC} but also forms a synchronization edge with a corresponding {@link #ACQUIRE} operation.
     */
    RELEASE(SingleRelease),
    /**
     * Acts as both {@link #ACQUIRE} and {@link #RELEASE} on one operation.
     */
    ACQUIRE_RELEASE(SingleAcqRel),
    /**
     * Like {@link #ACQUIRE_RELEASE} but applying a global total order.
     */
    SEQUENTIALLY_CONSISTENT(SingleSeqCst),
    /**
     * Java {@code VOLATILE} access.
     */
    VOLATILE(GlobalSeqCst);

    private final AccessMode accessMode;

    MemoryAtomicityMode(AccessMode accessMode) {
        this.accessMode = accessMode;
    }

    public static MemoryAtomicityMode max(MemoryAtomicityMode a, MemoryAtomicityMode b) {
        return a.ordinal() > b.ordinal() ? a : b;
    }

    public static MemoryAtomicityMode min(MemoryAtomicityMode a, MemoryAtomicityMode b) {
        return a.ordinal() < b.ordinal() ? a : b;
    }

    /**
     * Get the closest corresponding access mode, which may have to be transformed further before use.
     *
     * @return the access mode (not {@code null})
     */
    public AccessMode getAccessMode() {
        return  accessMode;
    }

    /**
     * Get the read side of the given mode.
     *
     * @return the read side of the given mode
     */
    public MemoryAtomicityMode read() {
        switch (this) {
            case RELEASE:
                return MONOTONIC;
            case ACQUIRE_RELEASE:
            case SEQUENTIALLY_CONSISTENT:
                return ACQUIRE;
            default: return this;
        }
    }
}
