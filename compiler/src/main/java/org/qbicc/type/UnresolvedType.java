package org.qbicc.type;

/**
 * The type of any unresolved reference.
 */
public final class UnresolvedType extends ValueType {
    UnresolvedType(final TypeSystem typeSystem) {
        super(typeSystem, UnresolvedType.class.hashCode());
    }

    public boolean isComplete() {
        return false;
    }

    public long getSize() {
        return 0;
    }

    public int getAlign() {
        return 0;
    }

    @Override
    public boolean equals(ValueType other) {
        return this == other;
    }

    public StringBuilder toFriendlyString(final StringBuilder b) {
        return b.append("unresolved");
    }
}
