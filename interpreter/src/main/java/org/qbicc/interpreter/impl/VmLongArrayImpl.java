package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.memory.CompositeMemory;
import org.qbicc.interpreter.memory.LongArrayMemory;
import org.qbicc.interpreter.memory.MemoryFactory;

/**
 *
 */
final class VmLongArrayImpl extends VmArrayImpl {
    private final LongArrayMemory arrayMemory;

    VmLongArrayImpl(VmImpl vm, int size) {
        super(vm.longArrayClass, MemoryFactory.wrap(new long[size]));
        arrayMemory = (LongArrayMemory) ((CompositeMemory)getMemory()).getSubMemory(1);
    }

    VmLongArrayImpl(final VmLongArrayImpl original) {
        super(original);
        arrayMemory = (LongArrayMemory) ((CompositeMemory)getMemory()).getSubMemory(1);
    }

    @Override
    public int getLength() {
        return arrayMemory.getArray().length;
    }

    @Override
    public long[] getArray() {
        return arrayMemory.getArray();
    }

    @Override
    protected VmLongArrayImpl clone() {
        return new VmLongArrayImpl(this);
    }
}
