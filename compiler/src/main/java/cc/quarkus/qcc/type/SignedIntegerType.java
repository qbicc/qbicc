package cc.quarkus.qcc.type;

import cc.quarkus.qcc.graph.literal.IntegerLiteral;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
public final class SignedIntegerType extends IntegerType {
    SignedIntegerType(final TypeSystem typeSystem, final int size, final int align, final int minBits, final boolean const_) {
        super(typeSystem, SignedIntegerType.class.hashCode(), const_, size, align, minBits);
    }

    ValueType constructConst() {
        return new SignedIntegerType(typeSystem, size, align, minBits, true);
    }

    public SignedIntegerType asConst() {
        return (SignedIntegerType) super.asConst();
    }

    public boolean equals(final IntegerType other) {
        return other instanceof SignedIntegerType && getMinBits() == other.getMinBits() && typeSystem == other.typeSystem;
    }

    public SignedIntegerType getConstraintType() {
        return this;
    }

    public SignedIntegerType asSigned() {
        return this;
    }

    public UnsignedIntegerType asUnsigned() {
        switch (minBits) {
            case 8: return typeSystem.getUnsignedInteger8Type();
            case 16: return typeSystem.getUnsignedInteger16Type();
            case 32: return typeSystem.getUnsignedInteger32Type();
            case 64: return typeSystem.getUnsignedInteger64Type();
            default: {
                throw Assert.impossibleSwitchCase(minBits);
            }
        }
    }

    public ValueType join(final ValueType other) {
        if (other instanceof SignedIntegerType) {
            return join((SignedIntegerType) other);
        } else {
            return super.join(other);
        }
    }

    public SignedIntegerType join(final SignedIntegerType other) {
        boolean const_ = isConst() || other.isConst();
        if (minBits < other.minBits) {
            return const_ ? other.asConst() : other;
        } else {
            return const_ ? asConst() : this;
        }
    }

    public StringBuilder toString(final StringBuilder b) {
        return super.toString(b).append("s").append(minBits);
    }

    public StringBuilder toFriendlyString(final StringBuilder b) {
        return b.append('s').append(minBits);
    }

    public String toString(final IntegerLiteral literal) {
        return Long.toString(literal.longValue());
    }
}
