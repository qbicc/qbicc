package org.qbicc.interpreter.impl;

import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.interpreter.Memory;
import org.qbicc.interpreter.VmObject;

class Memory64Impl extends MemoryImpl {

    static final Memory64Impl EMPTY = new Memory64Impl(0);

    Memory64Impl(int dataSize) {
        super(dataSize, 3);
    }

    @Override
    public VmObject loadRef(int index, MemoryAtomicityMode mode) {
        checkAlign(index, 8);
        return loadRefAligned(index >>> 3, mode);
    }

    @Override
    public void storeRef(int index, VmObject value, MemoryAtomicityMode mode) {
        checkAlign(index, 8);
        storeRefAligned(index >>> 3, value, mode);
    }

    @Override
    public void storeMemory(int destIndex, Memory src, int srcIndex, int size) {
        // first data
        super.storeMemory(destIndex, src, srcIndex, size);
        // now refs
        // if the copy is not properly aligned, undef behavior
        srcIndex += alignGap(8, srcIndex);
        destIndex += alignGap(8, destIndex);
        storeMemoryAligned(destIndex >> 3, (MemoryImpl) src, srcIndex >>> 3, size >>> 3);
    }

    void clearRefs(final int startIdx, final int byteSize) {
        clearRefsAligned(startIdx >> 3, (byteSize + 7) >> 3);
    }

    @Override
    public VmObject compareAndExchangeRef(int index, VmObject expect, VmObject update, MemoryAtomicityMode mode) {
        MemoryImpl.checkAlign(index, 8);
        return (VmObject) compareAndExchangeRefAligned(index >>> 3, (Referenceable) expect, (Referenceable) update, mode);
    }

    @Override
    public VmObject getAndSetRef(int index, VmObject value, MemoryAtomicityMode mode) {
        MemoryImpl.checkAlign(index, 8);
        return (VmObject) getAndSetRefAligned(index >>> 3, (Referenceable) value, mode);
    }
}
