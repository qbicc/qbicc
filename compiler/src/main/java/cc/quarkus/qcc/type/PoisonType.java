package cc.quarkus.qcc.type;

import io.smallrye.common.constraint.Assert;

/**
 * A type representing an invalid join or a value which cannot be used.
 */
public final class PoisonType extends ValueType {
    PoisonType(final TypeSystem typeSystem) {
        super(typeSystem, PoisonType.class.hashCode(), true);
    }

    public boolean isComplete() {
        return false;
    }

    public long getSize() {
        return 1;
    }

    ValueType constructConst() {
        throw Assert.unsupported();
    }

    public PoisonType asConst() {
        return this;
    }

    public int getAlign() {
        return 1;
    }

    public boolean equals(final ValueType other) {
        return other instanceof PoisonType && super.equals(other);
    }

    public StringBuilder toString(final StringBuilder b) {
        return b.append("poison");
    }

    public StringBuilder toFriendlyString(final StringBuilder b) {
        return toString(b);
    }
}
