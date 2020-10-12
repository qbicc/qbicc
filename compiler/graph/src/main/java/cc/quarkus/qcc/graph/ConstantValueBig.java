package cc.quarkus.qcc.graph;

import io.smallrye.common.constraint.Assert;

final class ConstantValueBig extends AbstractValue implements ConstantValue {
    private final byte[] value;
    private final Type type;

    ConstantValueBig(final byte[] value, final Type type) {
        this.value = value;
        this.type = type;
    }

    byte[] getValue() {
        return value;
    }

    public Type getType() {
        return type;
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
        for (byte b : value) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }

    public boolean isOne() {
        return type.isOne(value);
    }

    public boolean isNegative() {
        throw Assert.unsupported();
    }

    public boolean isNotNegative() {
        throw Assert.unsupported();
    }

    public boolean isPositive() {
        throw Assert.unsupported();
    }

    public boolean isNotPositive() {
        throw Assert.unsupported();
    }

    public ConstantValue withTypeRaw(final Type type) {
        return new ConstantValueBig(value, type);
    }

    public int compareTo(final ConstantValue other) throws IllegalArgumentException {
        if (other.getType() != type) {
            throw new IllegalArgumentException("Type mismatch");
        }
        if (type instanceof ComparableWordType) {
            throw Assert.unsupported();
        } else {
            throw new IllegalArgumentException("Type is not comparable");
        }
    }
}
