package cc.quarkus.qcc.graph;

/**
 * The mode for a field access operation.
 */
public enum MemoryAccessMode {
    /**
     * Detect access mode from the type declaration.
     */
    DETECT,
    /**
     * Plain access.
     */
    PLAIN,
    /**
     * Volatile access (in the C sense: the number or order of executions cannot be changed).
     */
    VOLATILE,
    ;
}
