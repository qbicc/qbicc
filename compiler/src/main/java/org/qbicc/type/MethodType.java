package org.qbicc.type;

import java.util.List;

/**
 * The type of any kind of method.  Method types imply an implicit parameter which carries the current
 * thread.
 */
public abstract class MethodType extends InvokableType {
    MethodType(final TypeSystem typeSystem, final int hashCode, final ValueType returnType, final List<ValueType> paramTypes) {
        super(typeSystem, hashCode, returnType, paramTypes);
    }

    @Override
    public abstract MethodType withReturnType(ValueType returnType);

    @Override
    public abstract MethodType withParameterTypes(List<ValueType> parameterTypes);

    @Override
    public MethodType withFirstParameterType(ValueType type) {
        return (MethodType) super.withFirstParameterType(type);
    }

    @Override
    public MethodType trimLastParameter() throws IndexOutOfBoundsException {
        return (MethodType) super.trimLastParameter();
    }

    @Override
    public boolean equals(InvokableType other) {
        return other instanceof MethodType mt && equals(mt);
    }

    public boolean equals(MethodType other) {
        return super.equals(other);
    }
}
