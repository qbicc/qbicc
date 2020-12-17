package cc.quarkus.qcc.type;

import io.smallrye.common.constraint.Assert;

/**
 * A type that represents the type of a value that is itself a type.  Used to represent {@code Class<? extends object>}.
 * Values of this type are considered incomplete and cannot be stored or lowered.
 * <p>
 * To represent the identifier of a class or interface type, see {@link TypeIdType}.
 */
public final class TypeType extends ValueType {
    private final ValueType upperBound;

    TypeType(final TypeSystem typeSystem, final ValueType upperBound) {
        super(typeSystem, TypeType.class.hashCode(), true);
        this.upperBound = upperBound;
    }

    public boolean isComplete() {
        return false;
    }

    public long getSize() {
        return 1;
    }

    public ValueType getUpperBound() {
        return upperBound;
    }

    ValueType constructConst() {
        throw Assert.unsupported();
    }

    public int getAlign() {
        return 1;
    }

    public StringBuilder toString(final StringBuilder b) {
        return upperBound.toString(b.append("typeof").append('.'));
    }

    public StringBuilder toFriendlyString(final StringBuilder b) {
        return upperBound.toString(b.append("typeof").append('.'));
    }
}
