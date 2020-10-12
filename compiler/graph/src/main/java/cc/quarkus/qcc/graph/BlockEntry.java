package cc.quarkus.qcc.graph;

/**
 * A node representing block entry.  Block entry nodes have no dependencies.
 */
public final class BlockEntry extends AbstractNode implements PinnedNode, Action {
    private final BlockLabel blockLabel;

    BlockEntry(final BlockLabel blockLabel) {
        this.blockLabel = blockLabel;
    }

    public BlockLabel getPinnedBlockLabel() {
        return blockLabel;
    }

    public <T, R> R accept(final ActionVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
