package org.qbicc.graph;

import java.util.NoSuchElementException;

/**
 * A node which may be ordered after another node in the program order.
 */
public interface OrderedNode extends Node {
    /**
     * Determine whether this node has a program-ordered predecessor.
     *
     * @return {@code true} if this node has a program-ordered predecessor, {@code false} otherwise
     */
    default boolean hasDependency() {
        return true;
    }

    /**
     * Get the program-ordered predecessor of this node, which may or may not in turn be
     * program-ordered.
     *
     * @return the predecessor (must not be {@code null})
     * @throws NoSuchElementException if there is no program-ordered predecessor
     */
    Node getDependency() throws NoSuchElementException;
}
