package org.qbicc.pointer;

import org.qbicc.interpreter.Memory;
import org.qbicc.interpreter.VmObject;

/**
 * A reference to an object which is converted into a pointer.
 */
public final class ReferenceAsPointer extends RootPointer {
    private final VmObject reference;

    public ReferenceAsPointer(VmObject reference) {
        super(reference.getObjectType().getPointer());
        this.reference = reference;
    }

    public VmObject getReference() {
        return reference;
    }

    @Override
    public Memory getRootMemoryIfExists() {
        return reference.getMemory();
    }

    @Override
    public int hashCode() {
        return super.hashCode() * 19 + reference.hashCode();
    }

    @Override
    public boolean equals(RootPointer other) {
        return other instanceof ReferenceAsPointer rap && equals(rap);
    }

    public boolean equals(ReferenceAsPointer other) {
        return this == other || super.equals(other) && reference == other.reference;
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        return b.append("refToPtr(").append(reference).append(")");
    }

    public <T, R> R accept(final Visitor<T, R> visitor, final T t) {
        return visitor.visit(t, this);
    }
}
