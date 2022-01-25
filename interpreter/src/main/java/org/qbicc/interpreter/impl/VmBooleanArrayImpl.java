package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.memory.BooleanArrayMemory;
import org.qbicc.interpreter.memory.CompositeMemory;
import org.qbicc.interpreter.memory.MemoryFactory;

/**
 *
 */
final class VmBooleanArrayImpl extends VmArrayImpl {
    private final BooleanArrayMemory arrayMemory;

    VmBooleanArrayImpl(VmImpl vm, int size) {
        super(vm.booleanArrayClass, MemoryFactory.wrap(new boolean[size]));
        arrayMemory = (BooleanArrayMemory) ((CompositeMemory)getMemory()).getSubMemory(1);
    }

    VmBooleanArrayImpl(final VmBooleanArrayImpl original) {
        super(original);
        arrayMemory = (BooleanArrayMemory) ((CompositeMemory)getMemory()).getSubMemory(1);
    }

    @Override
    public int getLength() {
        return arrayMemory.getArray().length;
    }

    @Override
    public boolean[] getArray() {
        return arrayMemory.getArray();
    }

    @Override
    protected VmBooleanArrayImpl clone() {
        return new VmBooleanArrayImpl(this);
    }
}
