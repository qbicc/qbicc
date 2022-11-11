package org.qbicc.type;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A type corresponding to a thing which can be invoked.
 */
public abstract class InvokableType extends ValueType {
    protected final ValueType returnType;
    protected final List<ValueType> paramTypes;

    InvokableType(final TypeSystem typeSystem, final int hashCode, final ValueType returnType, final List<ValueType> paramTypes) {
        super(typeSystem, hashCode * 19 + Objects.hash(paramTypes, returnType));
        this.returnType = returnType;
        this.paramTypes = paramTypes;
    }

    public boolean equals(final ValueType other) {
        return other instanceof InvokableType && equals((InvokableType) other);
    }

    public boolean equals(final InvokableType other) {
        return this == other || other != null && returnType.equals(other.returnType) && paramTypes.equals(other.paramTypes);
    }

    public boolean isComplete() {
        return false;
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
        return paramTypes.get(index);
    }

    public ValueType getLastParameterType(int revIndex) throws IndexOutOfBoundsException {
        return paramTypes.get(paramTypes.size() - 1 - revIndex);
    }

    public InvokableType withFirstParameterType(final ValueType type) {
        List<ValueType> parameterTypes = getParameterTypes();
        if (parameterTypes.isEmpty()) {
            return withParameterTypes(List.of(type));
        } else {
            List<ValueType> newParams = new ArrayList<>(parameterTypes.size() + 1);
            newParams.add(type);
            newParams.addAll(parameterTypes);
            return withParameterTypes(newParams);
        }
    }

    public InvokableType trimLastParameter() throws IndexOutOfBoundsException {
        List<ValueType> parameterTypes = getParameterTypes();
        return withParameterTypes(parameterTypes.subList(0, parameterTypes.size() - 1));
    }

    public int getParameterCount() {
        return paramTypes.size();
    }

    public List<ValueType> getParameterTypes() {
        return paramTypes;
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        b.append('(');
        List<ValueType> paramTypes = this.paramTypes;
        int length = paramTypes.size();
        if (length > 0) {
            b.append(paramTypes.get(0));
            for (int i = 1; i < length; i ++) {
                b.append(',').append(paramTypes.get(i));
            }
        }
        b.append(")");
        b.append(returnType);
        return b;
    }

    @Override
    public StringBuilder toFriendlyString(StringBuilder b) {
        return toString(b);
    }

    public abstract InvokableType withReturnType(ValueType returnType);

    public abstract InvokableType withParameterTypes(List<ValueType> parameterTypes);

    public boolean isVariadic() {
        return getParameterCount() > 0 && getLastParameterType(0) instanceof VariadicType;
    }
}
