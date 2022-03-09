package org.qbicc.type;

import java.util.List;

/**
 * The type of a native-format function.
 */
public final class FunctionType extends InvokableType {

    FunctionType(TypeSystem typeSystem, ValueType returnType, List<ValueType> paramTypes) {
        super(typeSystem, 0, returnType, paramTypes);
    }

    @Override
    public FunctionType withReturnType(ValueType returnType) {
        return getTypeSystem().getFunctionType(returnType, getParameterTypes());
    }

    @Override
    public FunctionType withParameterTypes(List<ValueType> parameterTypes) {
        return getTypeSystem().getFunctionType(getReturnType(), parameterTypes);
    }

    @Override
    public boolean equals(InvokableType other) {
        return other instanceof FunctionType ft && equals(ft);
    }

    public boolean equals(final FunctionType other) {
        return super.equals(other);
    }

    public boolean isVariadic() {
        return getParameterCount() > 0 && getLastParameterType(0) instanceof VariadicType;
    }

    public FunctionType withFirstParameterType(final ValueType type) {
        return (FunctionType) super.withFirstParameterType(type);
    }

    public FunctionType trimLastParameter() throws IndexOutOfBoundsException {
        return (FunctionType) super.trimLastParameter();
    }

    public StringBuilder toString(final StringBuilder b) {
        b.append("function (");
        List<ValueType> paramTypes = this.paramTypes;
        int length = paramTypes.size();
        if (length > 0) {
            b.append(paramTypes.get(0));
            for (int i = 1; i < length; i ++) {
                b.append(',').append(paramTypes.get(i));
            }
        }
        b.append("):");
        b.append(returnType);
        return b;
    }
}
