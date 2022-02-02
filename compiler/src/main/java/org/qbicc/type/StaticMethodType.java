package org.qbicc.type;

import java.util.List;

/**
 * The type of a static method.
 */
public final class StaticMethodType extends MethodType {

    StaticMethodType(TypeSystem typeSystem, ValueType returnType, List<ValueType> paramTypes) {
        super(typeSystem, 0, returnType, paramTypes);
    }

    @Override
    public StaticMethodType withReturnType(ValueType returnType) {
        return getTypeSystem().getStaticMethodType(returnType, getParameterTypes());
    }

    @Override
    public StaticMethodType withParameterTypes(List<ValueType> parameterTypes) {
        return getTypeSystem().getStaticMethodType(getReturnType(), parameterTypes);
    }

    @Override
    public StaticMethodType withFirstParameterType(ValueType type) {
        return (StaticMethodType) super.withFirstParameterType(type);
    }

    @Override
    public StaticMethodType trimLastParameter() throws IndexOutOfBoundsException {
        return (StaticMethodType) super.trimLastParameter();
    }

    @Override
    public boolean equals(InvokableType other) {
        return other instanceof StaticMethodType smt && equals(smt);
    }

    public boolean equals(final StaticMethodType other) {
        return super.equals(other);
    }

    public StringBuilder toString(final StringBuilder b) {
        b.append("static method");
        return super.toString(b);
    }

    public StringBuilder toFriendlyString(final StringBuilder b) {
        b.append("sm.");
        returnType.toFriendlyString(b);
        b.append('.').append(paramTypes.size());
        for (ValueType paramType : paramTypes) {
            paramType.toFriendlyString(b.append('.'));
        }
        return b;
    }
}
