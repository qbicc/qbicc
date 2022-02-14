package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.memory.CompositeMemory;
import org.qbicc.interpreter.memory.IntArrayMemory;
import org.qbicc.interpreter.memory.MemoryFactory;

/**
 *
 */
final class VmIntArrayImpl extends VmArrayImpl {
    private final IntArrayMemory arrayMemory;

    VmIntArrayImpl(VmImpl vm, int size) {
        super(vm.intArrayClass, MemoryFactory.wrap(new int[size]));
        arrayMemory = (IntArrayMemory) ((CompositeMemory)getMemory()).getSubMemory(1);
    }

    VmIntArrayImpl(final VmIntArrayImpl original) {
        super(original);
        arrayMemory = (IntArrayMemory) ((CompositeMemory)getMemory()).getSubMemory(1);
    }

    @Override
    public int getLength() {
        return arrayMemory.getArray().length;
    }

    @Override
    public int[] getArray() {
        return arrayMemory.getArray();
    }

    @Override
    protected VmIntArrayImpl clone() {
        return new VmIntArrayImpl(this);
    }
}
