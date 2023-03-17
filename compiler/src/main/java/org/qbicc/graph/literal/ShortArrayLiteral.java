package org.qbicc.graph.literal;

import java.util.Arrays;

import org.qbicc.graph.Value;
import org.qbicc.type.ArrayType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.SignedIntegerType;

/**
 * A literal array of shorts (16-bit integers).  This is not a Java array object literal (use {@code ObjectLiteral}).
 */
public final class ShortArrayLiteral extends Literal {
    private final short[] values;
    private final ArrayType type;
    private final int hashCode;

    ShortArrayLiteral(final ArrayType type, final short[] values) {
        this.values = values;
        this.type = type;
        type.getElementType(IntegerType.class);
        hashCode = type.hashCode() * 19 + Arrays.hashCode(values);
    }

    public short[] getValues() {
        return values;
    }

    public ArrayType getType() {
        return type;
    }

    public <T, R> R accept(final LiteralVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public Value extractElement(LiteralFactory lf, final Value index) {
        if (index instanceof IntegerLiteral il) {
            final int realIndex = il.intValue();
            if (0 <= realIndex && realIndex < values.length) {
                final short realVal = values[realIndex];
                if (type.getElementType() instanceof IntegerType it) {
                    if (it instanceof SignedIntegerType) {
                        return new IntegerLiteral(it, realVal);
                    } else {
                        return new IntegerLiteral(it, Short.toUnsignedInt(realVal));
                    }
                }
            }
        }
        return null;
    }

    public boolean isZero() {
        return false;
    }

    public boolean equals(final Literal other) {
        return other instanceof ShortArrayLiteral && equals((ShortArrayLiteral) other);
    }

    public boolean equals(final ShortArrayLiteral other) {
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
            target.append(Integer.toHexString(Short.toUnsignedInt(values[0])));
            for (int i = 1; i < values.length; i ++) {
                target.append(',');
                target.append(Integer.toHexString(Short.toUnsignedInt(values[i])));
            }
        }
        target.append(']');
        return target;
    }
}
