package org.qbicc.pointer;

import org.qbicc.interpreter.Memory;
import org.qbicc.type.PointerType;

/**
 * A pointer to some allocated interpreter memory block.
 */
public final class MemoryPointer extends RootPointer {
    private final Memory memory;

    public MemoryPointer(PointerType type, Memory memory) {
        super(type);
        this.memory = memory;
    }

    public Memory getMemory() {
        return memory;
    }

    @Override
    public Memory getRootMemoryIfExists() {
        return getMemory();
    }

    @Override
    public int hashCode() {
        return super.hashCode() * 19 + memory.hashCode();
    }

    @Override
    public boolean equals(RootPointer other) {
        return other instanceof MemoryPointer mp && equals(mp);
    }

    public boolean equals(MemoryPointer other) {
        return this == other || super.equals(other) && memory == other.memory;
    }

    @Override
    public String toString() {
        return "{memory}";
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        return b.append(this);
    }

    public <T, R> R accept(final Visitor<T, R> visitor, final T t) {
        return visitor.visit(t, this);
    }
}
