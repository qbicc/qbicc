package org.qbicc.graph;

/**
 * The mode for a Java field or array element access operation.
 */
public enum JavaAccessMode {
    /**
     * Detect access mode from field declaration.
     */
    DETECT,
    /**
     * Plain (opaque) access.
     */
    PLAIN,
    /**
     * Ordered access.
     */
    ORDERED,
    /**
     * Volatile access.
     */
    VOLATILE,
    ;
}
