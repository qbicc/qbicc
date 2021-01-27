package cc.quarkus.qcc.type.descriptor;

import java.util.Objects;

import io.smallrye.common.constraint.Assert;

/**
 * A holder for the unresolved method handle constant information.  There is a subclass for each shape that can
 * occur in a class file.
 */
public abstract class MethodHandleDescriptor extends Descriptor {
    private final ClassTypeDescriptor ownerDescriptor;
    private final MethodHandleKind kind;

    MethodHandleDescriptor(final int hashCode, final ClassTypeDescriptor ownerDescriptor, final MethodHandleKind kind) {
        super(Objects.hash(ownerDescriptor, kind) * 19 + hashCode);
        this.ownerDescriptor = Assert.checkNotNullParam("ownerDescriptor", ownerDescriptor);
        this.kind = Assert.checkNotNullParam("kind", kind);
    }

    public final boolean equals(final Descriptor other) {
        return other instanceof MethodHandleDescriptor && equals((MethodHandleDescriptor) other);
    }

    public boolean equals(final MethodHandleDescriptor other) {
        return super.equals(other) && ownerDescriptor.equals(other.ownerDescriptor) && kind.equals(other.kind);
    }

    public ClassTypeDescriptor getOwnerDescriptor() {
        return ownerDescriptor;
    }

    public MethodHandleKind getKind() {
        return kind;
    }
}
