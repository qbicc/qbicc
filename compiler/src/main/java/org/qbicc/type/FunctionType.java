package org.qbicc.type;

import java.util.Arrays;
import java.util.Objects;

/**
 *
 */
public final class FunctionType extends ValueType {
    private final ValueType returnType;
    private final ValueType[] paramTypes;

    FunctionType(final TypeSystem typeSystem, final ValueType returnType, final ValueType[] paramTypes) {
        super(typeSystem, Objects.hash((Object[]) paramTypes) * 19 + returnType.hashCode());
        this.returnType = returnType;
        this.paramTypes = paramTypes;
    }

    public boolean equals(final ValueType other) {
        return other instanceof FunctionType && equals((FunctionType) other);
    }

    public boolean equals(final FunctionType other) {
        return other == this || super.equals(other) && returnType.equals(other.returnType) && Arrays.equals(paramTypes, other.paramTypes);
    }

    public boolean isComplete() {
        return false;
    }

    public boolean isVariadic() {
        return paramTypes.length > 0 && paramTypes[paramTypes.length - 1] instanceof VariadicType;
    }

    public long getSize() {
        throw new UnsupportedOperationException("Incomplete type");
    }

    public int getAlign() {
        return typeSystem.getFunctionAlignment();
    }

    public ValueType getReturnType() {
        return returnType;
    }

    public ValueType getParameterType(int index) throws IndexOutOfBoundsException {
        return paramTypes[index];
    }

    public int getParameterCount() {
        return paramTypes.length;
    }

    public FunctionType withReturnType(final ValueType returnType) {
        return getTypeSystem().getFunctionType(returnType, paramTypes);
    }

    public FunctionType trimLastParameter() throws IndexOutOfBoundsException {
        return getTypeSystem().getFunctionType(returnType, Arrays.copyOf(paramTypes, paramTypes.length - 1));
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

    public StringBuilder toFriendlyString(final StringBuilder b) {
        b.append("fn.");
        returnType.toFriendlyString(b);
        b.append('.').append(paramTypes.length);
        for (ValueType paramType : paramTypes) {
            paramType.toFriendlyString(b.append('.'));
        }
        return b;
    }
}
