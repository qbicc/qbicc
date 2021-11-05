package org.qbicc.graph.literal;

import java.util.Arrays;

import org.qbicc.graph.ValueVisitor;
import org.qbicc.type.ArrayType;

/**
 * A literal array of bytes.  This is not a Java array object literal (use {@code ObjectLiteral}).
 */
public final class ByteArrayLiteral extends Literal {
    private final byte[] values;
    private final ArrayType type;
    private final int hashCode;

    ByteArrayLiteral(final ArrayType type, final byte[] values) {
        this.values = values;
        this.type = type;
        hashCode = type.hashCode() * 19 + Arrays.hashCode(values);
    }

    public byte[] getValues() {
        return values;
    }

    public ArrayType getType() {
        return type;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public boolean isZero() {
        return false;
    }

    public boolean equals(final Literal other) {
        return other instanceof ByteArrayLiteral && equals((ByteArrayLiteral) other);
    }

    public boolean equals(final ByteArrayLiteral other) {
        return this == other || other != null && hashCode == other.hashCode && Arrays.equals(values, other.values) && type.equals(other.type);
    }

    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean isNullable() {
        return false;
    }

    public StringBuilder toString(StringBuilder target) {
        target.append('[');
        if (values.length > 0) {
            target.append(Integer.toHexString(Byte.toUnsignedInt(values[0])));
            for (int i = 1; i < values.length; i ++) {
                target.append(',');
                target.append(Integer.toHexString(Byte.toUnsignedInt(values[i])));
            }
        }
        target.append(']');
        return target;
    }
}
