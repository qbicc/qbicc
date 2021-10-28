package org.qbicc.type.methodhandle;

import java.util.Objects;

import io.smallrye.common.constraint.Assert;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.Descriptor;

/**
 * A holder for the unresolved method handle constant information.  There is a subclass for each shape that can
 * occur in a class file.
 */
public abstract class MethodHandleConstant {
    private final ClassTypeDescriptor ownerDescriptor;
    private final MethodHandleKind kind;
    private final int hashCode;

    MethodHandleConstant(final int hashCode, final ClassTypeDescriptor ownerDescriptor, final MethodHandleKind kind) {
        this.hashCode =  Objects.hash(ownerDescriptor, kind) * 19 + hashCode;
        this.ownerDescriptor = Assert.checkNotNullParam("ownerDescriptor", ownerDescriptor);
        this.kind = Assert.checkNotNullParam("kind", kind);
    }

    public abstract Descriptor getDescriptor();

    public final boolean equals(final Object other) {
        return other instanceof MethodHandleConstant && equals((MethodHandleConstant) other);
    }

    public boolean equals(final MethodHandleConstant other) {
        return ownerDescriptor.equals(other.ownerDescriptor) && kind.equals(other.kind);
    }

    public int hashCode() {
        return hashCode;
    }

    public ClassTypeDescriptor getOwnerDescriptor() {
        return ownerDescriptor;
    }

    public MethodHandleKind getKind() {
        return kind;
    }
}
