package cc.quarkus.qcc.type;

import java.util.Objects;

/**
 * A type that represents the type of a value that is itself a type.  Values of this type are lowered to type identifiers
 * once the full set of reachable types is determined.
 */
public final class TypeType extends ValueType {
    private final ValueType upperBound;

    TypeType(final TypeSystem typeSystem, final ValueType upperBound) {
        super(typeSystem, Objects.hash(TypeType.class, upperBound));
        this.upperBound = upperBound;
    }

    public boolean isComplete() {
        return true;
    }

    public long getSize() {
        return typeSystem.getTypeIdSize();
    }

    public int getAlign() {
        return typeSystem.getTypeIdAlignment();
    }

    public ValueType getUpperBound() {
        return upperBound;
    }

    public boolean equals(final ValueType other) {
        return other instanceof TypeType && equals((TypeType) other);
    }

    public boolean equals(final TypeType other) {
        return super.equals(other) && upperBound.equals(other.upperBound);
    }

    public StringBuilder toString(final StringBuilder b) {
        return upperBound.toString(b.append("typeof").append('.'));
    }

    public StringBuilder toFriendlyString(final StringBuilder b) {
        return upperBound.toFriendlyString(b.append("typeof").append('.'));
    }
}
