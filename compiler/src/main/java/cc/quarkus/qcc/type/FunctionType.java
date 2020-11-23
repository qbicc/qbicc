package cc.quarkus.qcc.type;

import java.util.Arrays;
import java.util.Objects;

/**
 *
 */
public final class FunctionType extends ValueType {
    private final Type returnType;
    private final Type[] paramTypes;

    FunctionType(final TypeSystem typeSystem, final Type returnType, final Type[] paramTypes) {
        super(typeSystem, Objects.hash((Object[]) paramTypes) * 19 + returnType.hashCode(), false);
        this.returnType = returnType;
        this.paramTypes = paramTypes;
    }

    public boolean equals(final ValueType other) {
        return other instanceof FunctionType && equals((FunctionType) other);
    }

    public boolean equals(final FunctionType other) {
        return other == this || super.equals(other) && returnType.equals(other.returnType) && Arrays.equals(paramTypes, other.paramTypes);
    }

    public ValueType asConst() {
        throw new UnsupportedOperationException("Functions cannot be const");
    }

    public boolean isComplete() {
        return false;
    }

    public boolean isWritable() {
        return false;
    }

    public long getSize() {
        throw new UnsupportedOperationException("Incomplete type");
    }

    ValueType constructConst() {
        throw new UnsupportedOperationException();
    }

    public int getAlign() {
        return typeSystem.getFunctionAlignment();
    }

    public StringBuilder toString(final StringBuilder b) {
        b.append("function (");
        Type[] paramTypes = this.paramTypes;
        int length = paramTypes.length;
        if (length > 0) {
            b.append(paramTypes[0]);
            for (int i = 1; i < length; i ++) {
                b.append(',').append(paramTypes[i]);
            }
        }
        b.append("):");
        b.append(returnType);
        return b;
    }
}
