package org.qbicc.type;

/**
 * The type of a function parameter that is variadic.
 */
public final class VariadicType extends ValueType {
    VariadicType(TypeSystem typeSystem) {
        super(typeSystem, VariadicType.class.hashCode());
    }

    @Override
    public StringBuilder toFriendlyString(StringBuilder b) {
        return b.append("...");
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        return b.append("...");
    }

    @Override
    public boolean isComplete() {
        return false;
    }

    @Override
    public long getSize() {
        return 0;
    }

    @Override
    public int getAlign() {
        return 1;
    }

    @Override
    public boolean equals(ValueType other) {
        return this == other;
    }
}
