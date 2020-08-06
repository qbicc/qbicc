package cc.quarkus.qcc.graph;

/**
 *
 */
public interface PhiDependency extends PinnedNode {
    Node getDependencyForBlock(BasicBlock input);
    void setDependencyForBlock(BasicBlock input, Node dependency);
}
