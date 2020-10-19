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

    public SignedIntegerType getConstraintType() {
        return this;
    }

    public SignedIntegerType asSigned() {
        return this;
    }

    public boolean isClass2Type() {
        return minBits == 64;
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

    public String toString(final IntegerLiteral literal) {
        return Long.toString(literal.longValue());
    }
}
