package cc.quarkus.qcc.type;

import io.smallrye.common.constraint.Assert;

/**
 * The type representing string literals, which are always {@code const}, have no size and are incomplete.
 */
public final class StringType extends ValueType {
    StringType(final TypeSystem typeSystem) {
        super(typeSystem, StringType.class.hashCode(), true);
    }

    public boolean isComplete() {
        return false;
    }

    public long getSize() {
        return 0;
    }

    ValueType constructConst() {
        throw Assert.unreachableCode();
    }

    public StringType asConst() {
        return this;
    }

    public int getAlign() {
        return 0;
    }

    public boolean equals(final ValueType other) {
        return other instanceof StringType && super.equals(other);
    }

    public StringBuilder toString(final StringBuilder b) {
        return b.append("string");
    }

    public StringBuilder toFriendlyString(final StringBuilder b) {
        return b.append("str");
    }
}
