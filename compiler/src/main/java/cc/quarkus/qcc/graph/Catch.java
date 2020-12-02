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

    int calcHashCode() {
        return throwableType.hashCode();
    }

    public boolean equals(final Object other) {
        return other instanceof Catch && equals((Catch) other);
    }

    public boolean equals(final Catch other) {
        return this == other || other != null && throwableType.equals(other.throwableType);
    }
}
