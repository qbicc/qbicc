package org.qbicc.type.methodhandle;

import io.smallrye.common.constraint.Assert;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;

/**
 *
 */
public final class MethodMethodHandleConstant extends ExecutableMethodHandleConstant {
    private final String methodName;

    public MethodMethodHandleConstant(final ClassTypeDescriptor owner, final String methodName, final MethodHandleKind kind, final MethodDescriptor descriptor) {
        super(methodName.hashCode(), owner, kind, descriptor);
        if (! kind.isMethodTarget()) {
            throw new IllegalArgumentException("Method method handle cannot be of kind " + kind);
        }
        this.methodName = Assert.checkNotNullParam("methodName", methodName);
    }

    public String getMethodName() {
        return methodName;
    }

    public boolean equals(final ExecutableMethodHandleConstant other) {
        return other instanceof MethodMethodHandleConstant && equals((MethodMethodHandleConstant) other);
    }

    public boolean equals(final MethodMethodHandleConstant other) {
        return super.equals(other) && methodName.equals(other.methodName);
    }

    public StringBuilder toString(final StringBuilder target) {
        return target.append(getKind()).append('[').append(getOwnerDescriptor()).append('#').append(methodName).append(':').append(getDescriptor()).append(']');
    }
}
