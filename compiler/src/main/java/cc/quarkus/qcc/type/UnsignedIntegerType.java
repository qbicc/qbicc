package cc.quarkus.qcc.type;

import cc.quarkus.qcc.graph.literal.IntegerLiteral;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
public final class UnsignedIntegerType extends IntegerType {
    UnsignedIntegerType(final TypeSystem typeSystem, final int size, final int align, final int minBits, final boolean const_) {
        super(typeSystem, UnsignedIntegerType.class.hashCode(), const_, size, align, minBits);
    }

    ValueType constructConst() {
        return new UnsignedIntegerType(typeSystem, size, align, minBits, true);
    }

    public UnsignedIntegerType asConst() {
        return (UnsignedIntegerType) super.asConst();
    }

    public boolean equals(final IntegerType other) {
        return other instanceof UnsignedIntegerType && getMinBits() == other.getMinBits() && typeSystem == other.typeSystem;
    }

    public UnsignedIntegerType getConstraintType() {
        return this;
    }

    public UnsignedIntegerType asUnsigned() {
        return this;
    }

    public ValueType join(final ValueType other) {
        if (other instanceof UnsignedIntegerType) {
            return join((UnsignedIntegerType) other);
        } else {
            return super.join(other);
        }
    }

    public UnsignedIntegerType join(final UnsignedIntegerType other) {
        boolean const_ = isConst() || other.isConst();
        if (minBits < other.minBits) {
            return const_ ? other.asConst() : other;
        } else {
            return const_ ? asConst() : this;
        }
    }

    public StringBuilder toString(final StringBuilder b) {
        return super.toString(b).append("u").append(minBits);
    }

    public StringBuilder toFriendlyString(final StringBuilder b) {
        return b.append('u').append(minBits);
    }

    public String toString(final IntegerLiteral literal) {
        return Long.toUnsignedString(literal.longValue());
    }

    public SignedIntegerType asSigned() {
        switch (minBits) {
            case 8: return typeSystem.getSignedInteger8Type();
            case 16: return typeSystem.getSignedInteger16Type();
            case 32: return typeSystem.getSignedInteger32Type();
            case 64: return typeSystem.getSignedInteger64Type();
            default: {
                throw Assert.impossibleSwitchCase(minBits);
            }
        }
    }
}
