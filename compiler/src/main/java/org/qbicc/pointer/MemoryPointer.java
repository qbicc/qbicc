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

    public <T, R> R accept(final Visitor<T, R> visitor, final T t) {
        return visitor.visit(t, this);
    }
}
