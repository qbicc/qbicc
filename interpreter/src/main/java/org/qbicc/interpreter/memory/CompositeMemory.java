package org.qbicc.interpreter.memory;

import java.util.Arrays;

import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.interpreter.Memory;
import org.qbicc.interpreter.VmObject;
import org.qbicc.pointer.Pointer;
import org.qbicc.type.ValueType;

/**
 * A memory that is backed by multiple other concatenated memories of possibly varying sizes.
 */
public final class CompositeMemory implements Memory {
    private final Memory[] memories;
    private final long[] offsets;
    private final long size;

    CompositeMemory(Memory[] memories) {
        this.memories = memories;
        offsets = new long[memories.length];
        long offset = 0;
        for (int i = 0; i < memories.length; i ++) {
            offsets[i] = offset;
            offset += memories[i].getSize();
        }
        size = offset;
    }

    CompositeMemory(Memory[] memories, long[] offsets, long size) {
        this.memories = memories;
        this.offsets = offsets;
        this.size = size;
    }

    private int getDelegateIndex(final long index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException(index);
        }
        int which = Arrays.binarySearch(offsets, index);
        if (which < 0) {
            // we want the element *before* the insertion point
            which = -which - 2;
        }
        return which;
    }

    public Memory getSubMemory(int index) {
        return memories[index];
    }

    @Override
    public int load8(long index, ReadAccessMode mode) {
        int which = getDelegateIndex(index);
        return memories[which].load8(index - offsets[which], mode);
    }

    @Override
    public int load16(long index, ReadAccessMode mode) {
        int which = getDelegateIndex(index);
        return memories[which].load16(index - offsets[which], mode);
    }

    @Override
    public int load32(long index, ReadAccessMode mode) {
        int which = getDelegateIndex(index);
        return memories[which].load32(index - offsets[which], mode);
    }

    @Override
    public long load64(long index, ReadAccessMode mode) {
        int which = getDelegateIndex(index);
        return memories[which].load64(index - offsets[which], mode);
    }

    @Override
    public VmObject loadRef(long index, ReadAccessMode mode) {
        int which = getDelegateIndex(index);
        return memories[which].loadRef(index - offsets[which], mode);
    }

    @Override
    public ValueType loadType(long index, ReadAccessMode mode) {
        int which = getDelegateIndex(index);
        return memories[which].loadType(index - offsets[which], mode);
    }

    @Override
    public Pointer loadPointer(long index, ReadAccessMode mode) {
        int which = getDelegateIndex(index);
        return memories[which].loadPointer(index - offsets[which], mode);
    }

    @Override
    public void store8(long index, int value, WriteAccessMode mode) {
        int which = getDelegateIndex(index);
        memories[which].store8(index - offsets[which], value, mode);
    }

    @Override
    public void store16(long index, int value, WriteAccessMode mode) {
        int which = getDelegateIndex(index);
        memories[which].store16(index - offsets[which], value, mode);
    }

    @Override
    public void store32(long index, int value, WriteAccessMode mode) {
        int which = getDelegateIndex(index);
        memories[which].store32(index - offsets[which], value, mode);
    }

    @Override
    public void store64(long index, long value, WriteAccessMode mode) {
        int which = getDelegateIndex(index);
        memories[which].store64(index - offsets[which], value, mode);
    }

    @Override
    public void storeRef(long index, VmObject value, WriteAccessMode mode) {
        int which = getDelegateIndex(index);
        memories[which].storeRef(index - offsets[which], value, mode);
    }

    @Override
    public void storeType(long index, ValueType value, WriteAccessMode mode) {
        int which = getDelegateIndex(index);
        memories[which].storeType(index - offsets[which], value, mode);
    }

    @Override
    public void storePointer(long index, Pointer value, WriteAccessMode mode) {
        int which = getDelegateIndex(index);
        memories[which].storePointer(index - offsets[which], value, mode);
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
        int which = getDelegateIndex(index);
        return memories[which].compareAndExchange8(index - offsets[which], expect, update, readMode, writeMode);
    }

    @Override
    public int compareAndExchange16(long index, int expect, int update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].compareAndExchange16(index - offsets[which], expect, update, readMode, writeMode);
    }

    @Override
    public int compareAndExchange32(long index, int expect, int update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].compareAndExchange32(index - offsets[which], expect, update, readMode, writeMode);
    }

    @Override
    public long compareAndExchange64(long index, long expect, long update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].compareAndExchange64(index - offsets[which], expect, update, readMode, writeMode);
    }

    @Override
    public VmObject compareAndExchangeRef(long index, VmObject expect, VmObject update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].compareAndExchangeRef(index - offsets[which], expect, update, readMode, writeMode);
    }

    @Override
    public ValueType compareAndExchangeType(long index, ValueType expect, ValueType update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].compareAndExchangeType(index - offsets[which], expect, update, readMode, writeMode);
    }

    @Override
    public Pointer compareAndExchangePointer(long index, Pointer expect, Pointer update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].compareAndExchangePointer(index - offsets[which], expect, update, readMode, writeMode);
    }

    @Override
    public int getAndSet8(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].getAndSet8(index - offsets[which], value, readMode, writeMode);
    }

    @Override
    public int getAndSet16(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].getAndSet16(index - offsets[which], value, readMode, writeMode);
    }

    @Override
    public int getAndSet32(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].getAndSet32(index - offsets[which], value, readMode, writeMode);
    }

    @Override
    public long getAndSet64(long index, long value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].getAndSet64(index - offsets[which], value, readMode, writeMode);
    }

    @Override
    public VmObject getAndSetRef(long index, VmObject value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].getAndSetRef(index - offsets[which], value, readMode, writeMode);
    }

    @Override
    public ValueType getAndSetType(long index, ValueType value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].getAndSetType(index - offsets[which], value, readMode, writeMode);
    }

    @Override
    public int getAndAdd8(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].getAndAdd8(index - offsets[which], value, readMode, writeMode);
    }

    @Override
    public int getAndAdd16(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].getAndAdd16(index - offsets[which], value, readMode, writeMode);
    }

    @Override
    public int getAndAdd32(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].getAndAdd32(index - offsets[which], value, readMode, writeMode);
    }

    @Override
    public long getAndAdd64(long index, long value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].getAndAdd64(index - offsets[which], value, readMode, writeMode);
    }

    @Override
    public int getAndBitwiseAnd8(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].getAndBitwiseAnd8(index - offsets[which], value, readMode, writeMode);
    }

    @Override
    public int getAndBitwiseAnd16(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].getAndBitwiseAnd16(index - offsets[which], value, readMode, writeMode);
    }

    @Override
    public int getAndBitwiseAnd32(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].getAndBitwiseAnd32(index - offsets[which], value, readMode, writeMode);
    }

    @Override
    public long getAndBitwiseAnd64(long index, long value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].getAndBitwiseAnd64(index - offsets[which], value, readMode, writeMode);
    }

    @Override
    public int getAndBitwiseNand8(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].getAndBitwiseNand8(index - offsets[which], value, readMode, writeMode);
    }

    @Override
    public int getAndBitwiseNand16(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].getAndBitwiseNand16(index - offsets[which], value, readMode, writeMode);
    }

    @Override
    public int getAndBitwiseNand32(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].getAndBitwiseNand32(index - offsets[which], value, readMode, writeMode);
    }

    @Override
    public long getAndBitwiseNand64(long index, long value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].getAndBitwiseNand64(index - offsets[which], value, readMode, writeMode);
    }

    @Override
    public int getAndBitwiseOr8(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].getAndBitwiseOr8(index - offsets[which], value, readMode, writeMode);
    }

    @Override
    public int getAndBitwiseOr16(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].getAndBitwiseOr16(index - offsets[which], value, readMode, writeMode);
    }

    @Override
    public int getAndBitwiseOr32(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].getAndBitwiseOr32(index - offsets[which], value, readMode, writeMode);
    }

    @Override
    public long getAndBitwiseOr64(long index, long value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].getAndBitwiseOr64(index - offsets[which], value, readMode, writeMode);
    }

    @Override
    public int getAndBitwiseXor8(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].getAndBitwiseXor8(index - offsets[which], value, readMode, writeMode);
    }

    @Override
    public int getAndBitwiseXor16(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].getAndBitwiseXor16(index - offsets[which], value, readMode, writeMode);
    }

    @Override
    public int getAndBitwiseXor32(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].getAndBitwiseXor32(index - offsets[which], value, readMode, writeMode);
    }

    @Override
    public long getAndBitwiseXor64(long index, long value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].getAndBitwiseXor64(index - offsets[which], value, readMode, writeMode);
    }

    @Override
    public int getAndSetMaxSigned8(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].getAndSetMaxSigned8(index - offsets[which], value, readMode, writeMode);
    }

    @Override
    public int getAndSetMaxSigned16(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].getAndSetMaxSigned16(index - offsets[which], value, readMode, writeMode);
    }

    @Override
    public int getAndSetMaxSigned32(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].getAndSetMaxSigned32(index - offsets[which], value, readMode, writeMode);
    }

    @Override
    public long getAndSetMaxSigned64(long index, long value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].getAndSetMaxSigned64(index - offsets[which], value, readMode, writeMode);
    }

    @Override
    public int getAndSetMaxUnsigned8(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].getAndSetMaxUnsigned8(index - offsets[which], value, readMode, writeMode);
    }

    @Override
    public int getAndSetMaxUnsigned16(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].getAndSetMaxUnsigned16(index - offsets[which], value, readMode, writeMode);
    }

    @Override
    public int getAndSetMaxUnsigned32(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].getAndSetMaxUnsigned32(index - offsets[which], value, readMode, writeMode);
    }

    @Override
    public long getAndSetMaxUnsigned64(long index, long value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].getAndSetMaxUnsigned64(index - offsets[which], value, readMode, writeMode);
    }

    @Override
    public int getAndSetMinSigned8(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].getAndSetMinSigned8(index - offsets[which], value, readMode, writeMode);
    }

    @Override
    public int getAndSetMinSigned16(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].getAndSetMinSigned16(index - offsets[which], value, readMode, writeMode);
    }

    @Override
    public int getAndSetMinSigned32(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].getAndSetMinSigned32(index - offsets[which], value, readMode, writeMode);
    }

    @Override
    public long getAndSetMinSigned64(long index, long value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].getAndSetMinSigned64(index - offsets[which], value, readMode, writeMode);
    }

    @Override
    public int getAndSetMinUnsigned8(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].getAndSetMinUnsigned8(index - offsets[which], value, readMode, writeMode);
    }

    @Override
    public int getAndSetMinUnsigned16(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].getAndSetMinUnsigned16(index - offsets[which], value, readMode, writeMode);
    }

    @Override
    public int getAndSetMinUnsigned32(long index, int value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].getAndSetMinUnsigned32(index - offsets[which], value, readMode, writeMode);
    }

    @Override
    public long getAndSetMinUnsigned64(long index, long value, ReadAccessMode readMode, WriteAccessMode writeMode) {
        int which = getDelegateIndex(index);
        return memories[which].getAndSetMinUnsigned64(index - offsets[which], value, readMode, writeMode);
    }

    @Override
    public Memory copy(long newSize) {
        final int which = getDelegateIndex(newSize);
        if (which == 0) {
            return memories[0].copy(newSize);
        }
        Memory[] clones = new Memory[which + 1];
        for (int i = 0; i < which; i ++) {
            clones[i] = memories[i].clone();
        }
        clones[which] = memories[which].copy(newSize - offsets[which]);
        return new CompositeMemory(clones, Arrays.copyOf(offsets, which + 1), newSize);
    }

    @Override
    public Memory clone() {
        Memory[] clones = new Memory[memories.length];
        for (int i = 0; i < memories.length; i ++) {
            clones[i] = memories[i].clone();
        }
        return new CompositeMemory(clones, offsets, size);
    }

    @Override
    public Memory cloneZeroed() {
        Memory[] clones = new Memory[memories.length];
        for (int i = 0; i < memories.length; i ++) {
            clones[i] = memories[i].cloneZeroed();
        }
        return new CompositeMemory(clones, offsets, size);
    }

    @Override
    public long getSize() {
        return size;
    }
}
