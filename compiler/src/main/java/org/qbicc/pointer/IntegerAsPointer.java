package org.qbicc.pointer;

import org.qbicc.type.PointerType;

/**
 * A pointer value that is actually just an integer; used when a field of type {@code long} may hold a pointer value.
 */
public final class IntegerAsPointer extends RootPointer {
    private final long value;

    public IntegerAsPointer(PointerType type, long value) {
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

    @Override
    public int hashCode() {
        return super.hashCode() * 19 + Long.hashCode(value);
    }

    @Override
    public boolean equals(final RootPointer other) {
        return other instanceof IntegerAsPointer imp && equals(imp);
    }

    public boolean equals(final IntegerAsPointer other) {
        return this == other || super.equals(other) && value == other.value;
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        String hs = Long.toHexString(value);
        long ptrSize = getType().getSize();
        b.append("0x");
        int dc = (((int) ptrSize) << 1) - hs.length();
        for (int i = 0; i < dc; i ++) {
            b.append('0');
        }
        return b.append(hs);
    }


    public <T, R> R accept(final Visitor<T, R> visitor, final T t) {
        return visitor.visit(t, this);
    }
}
