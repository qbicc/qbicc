package org.qbicc.type;

import java.util.List;

/**
 * The type of an instance method.  In addition to the characteristics of method types, instance methods also
 * have a receiver type.  The receiver type is typically, but not always, a {@link ReferenceType}.
 */
public final class InstanceMethodType extends MethodType {
    private final ValueType receiverType;

    InstanceMethodType(TypeSystem typeSystem, ValueType receiverType, ValueType returnType, List<ValueType> paramTypes) {
        super(typeSystem, receiverType.hashCode(), returnType, paramTypes);
        this.receiverType = receiverType;
    }

    public ValueType getReceiverType() {
        return receiverType;
    }

    public InstanceMethodType withReceiverType(ValueType receiverType) {
        return getTypeSystem().getInstanceMethodType(receiverType, getReturnType(), getParameterTypes());
    }

    @Override
    public InstanceMethodType withReturnType(ValueType returnType) {
        return getTypeSystem().getInstanceMethodType(getReceiverType(), returnType, getParameterTypes());
    }

    @Override
    public InstanceMethodType withParameterTypes(List<ValueType> parameterTypes) {
        return getTypeSystem().getInstanceMethodType(getReceiverType(), getReturnType(), parameterTypes);
    }

    @Override
    public InstanceMethodType withFirstParameterType(ValueType type) {
        return (InstanceMethodType) super.withFirstParameterType(type);
    }

    @Override
    public InstanceMethodType trimLastParameter() throws IndexOutOfBoundsException {
        return (InstanceMethodType) super.trimLastParameter();
    }

    @Override
    public boolean equals(InvokableType other) {
        return other instanceof InstanceMethodType smt && equals(smt);
    }

    public boolean equals(final InstanceMethodType other) {
        return super.equals(other) && receiverType.equals(other.receiverType);
    }

    public StringBuilder toString(final StringBuilder b) {
        b.append("instance method ").append(receiverType);
        return super.toString(b);
    }

    public StringBuilder toFriendlyString(final StringBuilder b) {
        b.append("im.");
        receiverType.toFriendlyString(b);
        b.append('.');
        returnType.toFriendlyString(b);
        b.append('.').append(paramTypes.size());
        for (ValueType paramType : paramTypes) {
            paramType.toFriendlyString(b.append('.'));
        }
        return b;
    }
}
