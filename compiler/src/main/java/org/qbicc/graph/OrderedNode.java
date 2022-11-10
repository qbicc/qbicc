package org.qbicc.graph;

/**
 * A node which may be ordered after another node in the program order.
 */
public interface OrderedNode extends Node {

    /**
     * Get the program-ordered predecessor of this node, which may or may not in turn be
     * program-ordered.
     *
     * @return the predecessor (must not be {@code null})
     */
    Node getDependency();

    /**
     * Determine whether this node may safepoint.
     *
     * @return {@code true} if the node may safepoint, or {@code false} if the node <em>will not</em> safepoint
     */
    default boolean maySafePoint() {
        return false;
    }
}
