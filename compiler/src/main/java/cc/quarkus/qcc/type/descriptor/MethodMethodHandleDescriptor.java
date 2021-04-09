package org.qbicc.type.descriptor;

import java.util.Objects;

import io.smallrye.common.constraint.Assert;

/**
 *
 */
public final class MethodMethodHandleDescriptor extends MethodHandleDescriptor {
    private final String methodName;
    private final MethodDescriptor methodDescriptor;

    public MethodMethodHandleDescriptor(final ClassTypeDescriptor owner, final String methodName, final MethodHandleKind kind, final MethodDescriptor methodDescriptor) {
        super(Objects.hash(methodName, methodDescriptor), owner, kind);
        if (! kind.isMethodTarget()) {
            throw new IllegalArgumentException("Method method handle cannot be of kind " + kind);
        }
        this.methodDescriptor = Assert.checkNotNullParam("methodDescriptor", methodDescriptor);
        this.methodName = Assert.checkNotNullParam("methodName", methodName);
    }

    public String getMethodName() {
        return methodName;
    }

    public MethodDescriptor getMethodDescriptor() {
        return methodDescriptor;
    }

    public boolean equals(final MethodHandleDescriptor other) {
        return other instanceof MethodMethodHandleDescriptor && equals((MethodMethodHandleDescriptor) other);
    }

    public boolean equals(final MethodMethodHandleDescriptor other) {
        return super.equals(other) && methodName.equals(other.methodName) && methodDescriptor.equals(other.methodDescriptor);
    }

    public StringBuilder toString(final StringBuilder target) {
        return target.append(getKind()).append('[').append(getOwnerDescriptor()).append(':').append(methodName).append(methodDescriptor).append(']');
    }
}
