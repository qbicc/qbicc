package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.memory.CompositeMemory;
import org.qbicc.interpreter.memory.DoubleArrayMemory;
import org.qbicc.interpreter.memory.MemoryFactory;

/**
 *
 */
final class VmDoubleArrayImpl extends VmArrayImpl {
    private final DoubleArrayMemory arrayMemory;

    VmDoubleArrayImpl(VmImpl vm, int size) {
        super(vm.doubleArrayClass, MemoryFactory.wrap(new double[size]));
        arrayMemory = (DoubleArrayMemory) ((CompositeMemory)getMemory()).getSubMemory(1);
    }

    VmDoubleArrayImpl(final VmDoubleArrayImpl original) {
        super(original);
        arrayMemory = (DoubleArrayMemory) ((CompositeMemory)getMemory()).getSubMemory(1);
    }

    @Override
    public int getLength() {
        return arrayMemory.getArray().length;
    }

    @Override
    public double[] getArray() {
        return arrayMemory.getArray();
    }

    @Override
    protected VmDoubleArrayImpl clone() {
        return new VmDoubleArrayImpl(this);
    }
}
