package org.qbicc.type.descriptor;

import io.smallrye.common.constraint.Assert;

/**
 *
 */
public final class ConstructorMethodHandleDescriptor extends MethodHandleDescriptor {
    private final MethodDescriptor methodDescriptor;

    public ConstructorMethodHandleDescriptor(final ClassTypeDescriptor owner, final MethodHandleKind kind, final MethodDescriptor methodDescriptor) {
        super(methodDescriptor.hashCode(), owner, kind);
        if (! kind.isConstructorTarget()) {
            throw new IllegalArgumentException("Constructor method handle cannot be of kind " + kind);
        }
        this.methodDescriptor = Assert.checkNotNullParam("signature", methodDescriptor);
    }

    public MethodDescriptor getMethodDescriptor() {
        return methodDescriptor;
    }

    public final boolean equals(final MethodHandleDescriptor other) {
        return other instanceof ConstructorMethodHandleDescriptor && equals((ConstructorMethodHandleDescriptor) other);
    }

    public boolean equals(final ConstructorMethodHandleDescriptor other) {
        return super.equals(other) && methodDescriptor.equals(other.methodDescriptor);
    }

    public StringBuilder toString(final StringBuilder target) {
        return target.append(getKind()).append('[').append(getOwnerDescriptor()).append(':').append(methodDescriptor).append(']');
    }
}
