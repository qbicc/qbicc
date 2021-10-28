package org.qbicc.type.methodhandle;

import io.smallrye.common.constraint.Assert;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;

/**
 *
 */
public abstract class ExecutableMethodHandleConstant extends MethodHandleConstant {
    private final MethodDescriptor descriptor;

    ExecutableMethodHandleConstant(int hashCode, ClassTypeDescriptor ownerDescriptor, MethodHandleKind kind, MethodDescriptor descriptor) {
        super(hashCode * 19 + descriptor.hashCode(), ownerDescriptor, kind);
        this.descriptor = Assert.checkNotNullParam("descriptor", descriptor);
    }

    public MethodDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public final boolean equals(MethodHandleConstant other) {
        return other instanceof ExecutableMethodHandleConstant && equals((ExecutableMethodHandleConstant) other);
    }

    public boolean equals(ExecutableMethodHandleConstant other) {
        return super.equals(other) && descriptor.equals(other.descriptor);
    }
}
