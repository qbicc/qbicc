package org.qbicc.type.methodhandle;

import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;

/**
 *
 */
public final class ConstructorMethodHandleConstant extends ExecutableMethodHandleConstant {

    public ConstructorMethodHandleConstant(final ClassTypeDescriptor ownerDescriptor, final MethodHandleKind kind, final MethodDescriptor descriptor) {
        super(0, ownerDescriptor, kind, descriptor);
        if (! kind.isConstructorTarget()) {
            throw new IllegalArgumentException("Constructor method handle cannot be of kind " + kind);
        }
    }

    public boolean equals(final ExecutableMethodHandleConstant other) {
        return other instanceof ConstructorMethodHandleConstant && equals((ConstructorMethodHandleConstant) other);
    }

    public boolean equals(final ConstructorMethodHandleConstant other) {
        return super.equals(other);
    }

    public StringBuilder toString(final StringBuilder target) {
        return target.append(getKind()).append('[').append(getOwnerDescriptor()).append(':').append(getDescriptor()).append(']');
    }
}
