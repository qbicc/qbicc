package cc.quarkus.qcc.graph;

import io.smallrye.common.constraint.Assert;

final class BlockLiteralValue extends AbstractValue implements ConstantValue {
    private final BlockLabel blockLabel;

    BlockLiteralValue(final BlockLabel blockLabel) {
        this.blockLabel = blockLabel;
    }

    public long longValue() {
        throw Assert.unsupported();
    }

    public int intValue() {
        throw Assert.unsupported();
    }

    public short shortValue() {
        throw Assert.unsupported();
    }

    public byte byteValue() {
        throw Assert.unsupported();
    }

    public char charValue() {
        throw Assert.unsupported();
    }

    public boolean isZero() {
        return false;
    }

    public boolean isOne() {
        return false;
    }

    public boolean isNegative() {
        return false;
    }

    public boolean isNotNegative() {
        return false;
    }

    public boolean isPositive() {
        return false;
    }

    public boolean isNotPositive() {
        return false;
    }

    public ConstantValue withTypeRaw(final Type type) {
        throw Assert.unsupported();
    }

    public int compareTo(final ConstantValue other) throws IllegalArgumentException {
        throw Assert.unsupported();
    }

    public Type getType() {
        return ReturnAddressType.INSTANCE;
    }
}
