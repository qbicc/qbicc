package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.ReferenceType;
import cc.quarkus.qcc.type.ValueType;

/**
 * A caught value.
 */
public final class Catch extends AbstractValue implements PinnedNode {
    private final BlockLabel pinnedBlockLabel;
    private final ReferenceType throwableType;

    Catch(final BlockLabel pinnedBlockLabel, final ReferenceType throwableType) {
        this.pinnedBlockLabel = pinnedBlockLabel;
        this.throwableType = throwableType;
    }

    public BlockLabel getPinnedBlockLabel() {
        return pinnedBlockLabel;
    }

    public ValueType getType() {
        return throwableType;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
