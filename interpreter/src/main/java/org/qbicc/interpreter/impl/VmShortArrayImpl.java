package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.memory.CompositeMemory;
import org.qbicc.interpreter.memory.MemoryFactory;
import org.qbicc.interpreter.memory.ShortArrayMemory;

/**
 *
 */
final class VmShortArrayImpl extends VmArrayImpl {
    private final ShortArrayMemory arrayMemory;

    VmShortArrayImpl(VmImpl vm, int size) {
        super(vm.shortArrayClass, MemoryFactory.wrap(new short[size]));
        arrayMemory = (ShortArrayMemory) ((CompositeMemory)getMemory()).getSubMemory(1);
    }

    VmShortArrayImpl(final VmShortArrayImpl original) {
        super(original);
        arrayMemory = (ShortArrayMemory) ((CompositeMemory)getMemory()).getSubMemory(1);
    }

    @Override
    public int getLength() {
        return arrayMemory.getArray().length;
    }

    @Override
    public short[] getArray() {
        return arrayMemory.getArray();
    }

    @Override
    protected VmShortArrayImpl clone() {
        return new VmShortArrayImpl(this);
    }
}
