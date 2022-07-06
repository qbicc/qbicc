package org.qbicc.pointer;

import org.qbicc.interpreter.Memory;

/**
 * A pointer which is offset from the base pointer by a number of elements of the pointer's pointee type.
 */
public final class OffsetPointer extends Pointer {
    private final Pointer basePointer;
    private final long offset;

    OffsetPointer(Pointer basePointer, long offset) {
        super(basePointer.getType());
        this.basePointer = basePointer;
        this.offset = offset;
    }

    public Pointer getBasePointer() {
        return basePointer;
    }

    public long getOffset() {
        return offset;
    }

    @Override
    public Pointer offsetByElements(long count) {
        if (count == 0) {
            return this;
        } else if (count + offset == 0) {
            return basePointer;
        } else {
            return new OffsetPointer(basePointer, count + offset);
        }
    }

    @Override
    public RootPointer getRootPointer() {
        return basePointer.getRootPointer();
    }

    @Override
    public long getRootByteOffset() {
        return basePointer.getRootByteOffset() + offset * basePointer.getPointeeType().getSize();
    }

    @Override
    public Memory getRootMemoryIfExists() {
        return basePointer.getRootMemoryIfExists();
    }

    @Override
    public String getRootSymbolIfExists() {
        return basePointer.getRootSymbolIfExists();
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        return basePointer.toString(b.append('(')).append(')').append('+').append(offset);
    }

    public <T, R> R accept(final Visitor<T, R> visitor, final T t) {
        return visitor.visit(t, this);
    }
}
