package org.qbicc.pointer;

import org.qbicc.type.PointerType;

/**
 * A pointer value that is actually just an integer; used when a field of type {@code long} may hold a pointer value.
 */
public final class IntegerAsPointer extends RootPointer {
    private final long value;

    IntegerAsPointer(PointerType type, long value) {
        super(type);
        this.value = value;
    }

    public long getValue() {
        return value;
    }

    @Override
    public long getRootByteOffset() {
        return getValue();
    }

    public <T, R> R accept(final Visitor<T, R> visitor, final T t) {
        return visitor.visit(t, this);
    }
}
