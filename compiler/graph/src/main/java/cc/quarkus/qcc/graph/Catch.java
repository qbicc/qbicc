package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.ReferenceType;

/**
 * A caught value.
 */
public final class Catch extends AbstractValue {
    private final ReferenceType throwableType;

    Catch(final ReferenceType throwableType) {
        super(0, -1);
        this.throwableType = throwableType;
    }

    public ReferenceType getType() {
        return throwableType;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
