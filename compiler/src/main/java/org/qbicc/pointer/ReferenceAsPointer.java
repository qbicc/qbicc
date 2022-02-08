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

    public <T, R> R accept(final Visitor<T, R> visitor, final T t) {
        return visitor.visit(t, this);
    }
}
