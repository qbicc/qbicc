package org.qbicc.graph;

/**
 * The mode for a memory access operation.
 */
public enum MemoryAtomicityMode {
    /**
     * Non-atomic access (cannot be given for fence or read-modify-write operations).
     */
    NONE,
    /**
     * Plain access (cannot be given for fence or read-modify-write operations); any value that is read was written by
     * some other operation.
     */
    UNORDERED,
    /**
     * Single total order for a given memory address (cannot be given for fence operations).
     */
    MONOTONIC,
    /**
     * Like {@link #MONOTONIC} but also forms a synchronization edge with a corresponding {@link #RELEASE} operation.
     */
    ACQUIRE,
    /**
     * Like {@link #MONOTONIC} but also forms a synchronization edge with a corresponding {@link #ACQUIRE} operation.
     */
    RELEASE,
    /**
     * Acts as both {@link #ACQUIRE} and {@link #RELEASE} on one operation.
     */
    ACQUIRE_RELEASE,
    /**
     * Like {@link #ACQUIRE_RELEASE} but applying a global total order.
     */
    SEQUENTIALLY_CONSISTENT,
    /**
     * Java {@code VOLATILE} access.
     */
    VOLATILE
    ;

    public static MemoryAtomicityMode max(MemoryAtomicityMode a, MemoryAtomicityMode b) {
        return a.ordinal() > b.ordinal() ? a : b;
    }

    public static MemoryAtomicityMode min(MemoryAtomicityMode a, MemoryAtomicityMode b) {
        return a.ordinal() < b.ordinal() ? a : b;
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
