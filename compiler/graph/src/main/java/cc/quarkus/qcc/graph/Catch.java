package cc.quarkus.qcc.graph;

/**
 * A caught value.
 */
public final class Catch extends AbstractValue implements Value, PinnedNode {
    private final BlockLabel pinnedBlockLabel;
    private final ClassType throwableType;

    Catch(final BlockLabel pinnedBlockLabel, final ClassType throwableType) {
        this.pinnedBlockLabel = pinnedBlockLabel;
        this.throwableType = throwableType;
    }

    public BlockLabel getPinnedBlockLabel() {
        return pinnedBlockLabel;
    }

    public ClassType getType() {
        return throwableType;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
