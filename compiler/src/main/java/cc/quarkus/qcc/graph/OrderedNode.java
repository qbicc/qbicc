package cc.quarkus.qcc.graph;

/**
 * A node which is ordered after another node in the program order.
 */
public interface OrderedNode extends Node {
    /**
     * Get the program-ordered predecessor of this node, which may or may not in turn be
     * program-ordered.
     *
     * @return the predecessor (must not be {@code null})
     */
    Node getDependency();
}
