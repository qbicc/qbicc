package org.qbicc.type;

/**
 * The type of any unresolved reference.
 */
public final class UnresolvedType extends ValueType {
    UnresolvedType(final TypeSystem typeSystem) {
        super(typeSystem, UnresolvedType.class.hashCode());
    }

    public long getSize() {
        return typeSystem.getReferenceSize();
    }

    public int getAlign() {
        return typeSystem.getReferenceAlignment();
    }

    @Override
    public boolean equals(ValueType other) {
        return this == other;
    }

    public StringBuilder toFriendlyString(final StringBuilder b) {
        return b.append("unresolved");
    }
}
