package org.qbicc.type.definition;

import java.util.List;
import java.util.Objects;

import org.qbicc.type.descriptor.MethodDescriptor;

/**
 * An identifier for a method type, which corresponds to the {@code java.lang.invoke} equivalent but on the compiler side.
 */
public final class MethodTypeId {
    private final List<TypeId> parameterTypes;
    private final TypeId returnType;
    private final int hashCode;
    private final MethodDescriptor descriptor;

    /**
     * Construct a new instance. Use {@link org.qbicc.context.ClassContext#resolveMethodType(MethodDescriptor)} instead.
     *
     * @param parameterTypes the parameter type list (must not be {@code null})
     * @param returnType the return type (must not be {@code null})
     * @param descriptor the corresponding descriptor (must not be {@code null})
     */
    public MethodTypeId(List<TypeId> parameterTypes, TypeId returnType, MethodDescriptor descriptor) {
        this.parameterTypes = parameterTypes;
        this.returnType = returnType;
        this.descriptor = descriptor;
        hashCode = Objects.hash(parameterTypes, returnType);
    }

    /**
     * Get the types of the parameters of this method type.
     *
     * @return the parameter types (not {@code null})
     */
    public List<TypeId> getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Get the return type of this method type.
     *
     * @return the return type (not {@code null})
     */
    public TypeId getReturnType() {
        return returnType;
    }

    /**
     * Get the method descriptor that corresponds to this method type.
     * More than one method type might correspond to the same descriptor.
     *
     * @return the method descriptor (not {@code null})
     */
    public MethodDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MethodTypeId mt && equals(mt);
    }

    public boolean equals(MethodTypeId other) {
        return this == other || other != null && returnType.equals(other.returnType) && parameterTypes.equals(other.parameterTypes);
    }
}
