package cc.quarkus.qcc.type;

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

    public UnsignedIntegerType getConstraintType() {
        return this;
    }

    public UnsignedIntegerType asUnsigned() {
        return this;
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
