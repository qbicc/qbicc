package org.qbicc.interpreter.impl;

import java.util.Arrays;

import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.interpreter.Memory;
import org.qbicc.interpreter.VmObject;

class Memory32Impl extends MemoryImpl {

    static final Memory32Impl EMPTY = new Memory32Impl(0);

    Memory32Impl(int dataSize) {
        super(dataSize, 2);
    }

    @Override
    public VmObject loadRef(int index, MemoryAtomicityMode mode) {
        checkAlign(index, 4);
        return loadRefAligned(index >>> 2, mode);
    }

    @Override
    public void storeRef(int index, VmObject value, MemoryAtomicityMode mode) {
        checkAlign(index, 4);
        storeRefAligned(index >>> 2, value, mode);
    }

    @Override
    public void storeMemory(int destIndex, Memory src, int srcIndex, int size) {
        // first data
        super.storeMemory(destIndex, src, srcIndex, size);
        // now refs
        // if the copy is not properly aligned, undef behavior
        srcIndex += alignGap(4, srcIndex);
        destIndex += alignGap(4, destIndex);
        storeMemoryAligned(destIndex >> 2, (MemoryImpl) src, srcIndex >>> 2, size >>> 2);
    }

    void clearRefs(final int startIdx, final int byteSize) {
        clearRefsAligned(startIdx >> 2, (byteSize + 3) >> 2);
    }

    @Override
    public VmObject compareAndExchangeRef(int index, VmObject expect, VmObject update, MemoryAtomicityMode mode) {
        MemoryImpl.checkAlign(index, 4);
        return (VmObject) compareAndExchangeRefAligned(index >>> 2, (Referenceable) expect, (Referenceable) update, mode);
    }

    @Override
    public VmObject getAndSetRef(int index, VmObject value, MemoryAtomicityMode mode) {
        MemoryImpl.checkAlign(index, 4);
        return (VmObject) getAndSetRefAligned(index >>> 2, (Referenceable) value, mode);
    }
}
