package cc.quarkus.qcc.graph;

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
}
