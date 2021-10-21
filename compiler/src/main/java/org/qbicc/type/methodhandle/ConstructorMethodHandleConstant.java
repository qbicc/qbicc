package org.qbicc.type.methodhandle;

import io.smallrye.common.constraint.Assert;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;

/**
 *
 */
public final class ConstructorMethodHandleConstant extends MethodHandleConstant {
    private final MethodDescriptor methodDescriptor;

    public ConstructorMethodHandleConstant(final ClassTypeDescriptor owner, final MethodHandleKind kind, final MethodDescriptor methodDescriptor) {
        super(methodDescriptor.hashCode(), owner, kind);
        if (! kind.isConstructorTarget()) {
            throw new IllegalArgumentException("Constructor method handle cannot be of kind " + kind);
        }
        this.methodDescriptor = Assert.checkNotNullParam("signature", methodDescriptor);
    }

    public MethodDescriptor getMethodDescriptor() {
        return methodDescriptor;
    }

    public boolean equals(final MethodHandleConstant other) {
        return other instanceof ConstructorMethodHandleConstant && equals((ConstructorMethodHandleConstant) other);
    }

    public boolean equals(final ConstructorMethodHandleConstant other) {
        return super.equals(other) && methodDescriptor.equals(other.methodDescriptor);
    }

    public StringBuilder toString(final StringBuilder target) {
        return target.append(getKind()).append('[').append(getOwnerDescriptor()).append(':').append(methodDescriptor).append(']');
    }
}
