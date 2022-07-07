package org.qbicc.interpreter.memory;

import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.interpreter.Memory;
import org.qbicc.interpreter.VmObject;
import org.qbicc.pointer.Pointer;
import org.qbicc.type.ValueType;

/**
 * A memory which is backed by an array of a uniform delegate memory.
 */
public final class VectorMemory extends AbstractMemory {
    private final long divisor;
    private final Memory[] memories;

    /**
     * Create a memory backed by a given memory followed by {@code count - 1}
     * clones of the given memory.
     *
     * @param first the first memory in the array (must not be {@code null})
     * @param count the number of copies to make.
     */
    VectorMemory(Memory first, int count) {
        if (count < 1) {
            throw new IllegalArgumentException("Must have at least one element");
        }
        memories = new Memory[count];
        memories[0] = first;
        for (int i = 1; i < count; i ++) {
            memories[i] = first.clone();
        }
        divisor = first.getSize();
    }

    VectorMemory(final VectorMemory orig, final boolean zero) {
        divisor = orig.divisor;
        final int count = orig.memories.length;
        memories = new Memory[count];
        for (int i = 0; i < count; i ++) {
            final Memory origMemory = orig.memories[i];
            memories[i] = zero ? origMemory.cloneZeroed() : origMemory.clone();
        }
    }

    VectorMemory(final VectorMemory orig, final int newCount) {
        divisor = orig.divisor;
        final int oldCount = orig.memories.length;
        final int count = Math.min(oldCount, newCount);
        memories = new Memory[newCount];
        for (int i = 0; i < count; i ++) {
            memories[i] = orig.memories[i].clone();
        }
        for (int i = oldCount; i < newCount; i ++) {
            memories[i] = orig.memories[oldCount - 1].cloneZeroed();
        }
    }

    @Override
    public int load8(long index, ReadAccessMode mode) {
        int number = (int) (index / divisor);
        long offset = index % divisor;
        return memories[number].load8(offset, mode);
    }

    @Override
    public int load16(long index, ReadAccessMode mode) {
        int number = (int) (index / divisor);
        long offset = index % divisor;
        return memories[number].load16(offset, mode);
    }

    @Override
    public int load32(long index, ReadAccessMode mode) {
        int number = (int) (index / divisor);
        long offset = index % divisor;
        return memories[number].load32(offset, mode);
    }

    @Override
    public long load64(long index, ReadAccessMode mode) {
        int number = (int) (index / divisor);
        long offset = index % divisor;
        return memories[number].load64(offset, mode);
    }

    @Override
    public VmObject loadRef(long index, ReadAccessMode mode) {
        int number = (int) (index / divisor);
        long offset = index % divisor;
        return memories[number].loadRef(offset, mode);
    }

    @Override
    public ValueType loadType(long index, ReadAccessMode mode) {
        int number = (int) (index / divisor);
        long offset = index % divisor;
        return memories[number].loadType(offset, mode);
    }

    @Override
    public Pointer loadPointer(long index, ReadAccessMode mode) {
        int number = (int) (index / divisor);
        long offset = index % divisor;
        return memories[number].loadPointer(offset, mode);
    }

    @Override
    public void store8(long index, int value, WriteAccessMode mode) {
        int number = (int) (index / divisor);
        long offset = index % divisor;
        memories[number].store8(offset, value, mode);
    }

    @Override
    public void store16(long index, int value, WriteAccessMode mode) {
        int number = (int) (index / divisor);
        long offset = index % divisor;
        memories[number].store16(offset, value, mode);
    }

    @Override
    public void store32(long index, int value, WriteAccessMode mode) {
        int number = (int) (index / divisor);
        long offset = index % divisor;
        memories[number].store32(offset, value, mode);
    }

    @Override
    public void store64(long index, long value, WriteAccessMode mode) {
        int number = (int) (index / divisor);
        long offset = index % divisor;
        memories[number].store64(offset, value, mode);
    }

    @Override
    public void storeRef(long index, VmObject value, WriteAccessMode mode) {
        int number = (int) (index / divisor);
        long offset = index % divisor;
        memories[number].storeRef(offset, value, mode);
    }

    @Override
    public void storeType(long index, ValueType value, WriteAccessMode mode) {
        int number = (int) (index / divisor);
        long offset = index % divisor;
        memories[number].storeType(offset, value, mode);
    }

    @Override
    public void storePointer(long index, Pointer value, WriteAccessMode mode) {
        int number = (int) (index / divisor);
        long offset = index % divisor;
        memories[number].storePointer(offset, value, mode);
    }

    @Override
    public void storeMemory(long destIndex, Memory src, long srcIndex, long size) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void storeMemory(long destIndex, byte[] src, int srcIndex, int size) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int compareAndExchange8(long index, int expect, int update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int number = (int) (index / divisor);
        long offset = index % divisor;
        return memories[number].compareAndExchange8(offset, expect, update, readMode, writeMode);
    }

    @Override
    public int compareAndExchange16(long index, int expect, int update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int number = (int) (index / divisor);
        long offset = index % divisor;
        return memories[number].compareAndExchange16(offset, expect, update, readMode, writeMode);
    }

    @Override
    public int compareAndExchange32(long index, int expect, int update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int number = (int) (index / divisor);
        long offset = index % divisor;
        return memories[number].compareAndExchange32(offset, expect, update, readMode, writeMode);
    }

    @Override
    public long compareAndExchange64(long index, long expect, long update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int number = (int) (index / divisor);
        long offset = index % divisor;
        return memories[number].compareAndExchange64(offset, expect, update, readMode, writeMode);
    }

    @Override
    public VmObject compareAndExchangeRef(long index, VmObject expect, VmObject update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int number = (int) (index / divisor);
        long offset = index % divisor;
        return memories[number].compareAndExchangeRef(offset, expect, update, readMode, writeMode);
    }

    @Override
    public ValueType compareAndExchangeType(long index, ValueType expect, ValueType update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int number = (int) (index / divisor);
        long offset = index % divisor;
        return memories[number].compareAndExchangeType(offset, expect, update, readMode, writeMode);
    }

    @Override
    public Pointer compareAndExchangePointer(long index, Pointer expect, Pointer update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int number = (int) (index / divisor);
        long offset = index % divisor;
        return memories[number].compareAndExchangePointer(offset, expect, update, readMode, writeMode);
    }

    @Override
    public Memory copy(long newSize) {
        // round up
        return new VectorMemory(this, (int) ((newSize + divisor - 1) / divisor));
    }

    @Override
    public Memory clone() {
        return new VectorMemory(this, false);
    }

    @Override
    public Memory cloneZeroed() {
        return new VectorMemory(this, true);
    }

    @Override
    public long getSize() {
        return memories.length * divisor;
    }
}
