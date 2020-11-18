package cc.quarkus.qcc.type;

import java.util.Arrays;
import java.util.Objects;

/**
 *
 */
public final class FunctionType extends Type {
    private final Type returnType;
    private final Type[] paramTypes;

    FunctionType(final TypeSystem typeSystem, final Type returnType, final Type[] paramTypes) {
        super(typeSystem, Objects.hash((Object[]) paramTypes) * 19 + returnType.hashCode());
        this.returnType = returnType;
        this.paramTypes = paramTypes;
    }

    public boolean equals(final Type other) {
        return other instanceof FunctionType && equals((FunctionType) other);
    }

    public boolean equals(final FunctionType other) {
        return other == this || super.equals(other) && returnType.equals(other.returnType) && Arrays.equals(paramTypes, other.paramTypes);
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
