package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.memory.CompositeMemory;
import org.qbicc.interpreter.memory.FloatArrayMemory;
import org.qbicc.interpreter.memory.MemoryFactory;

/**
 *
 */
final class VmFloatArrayImpl extends VmArrayImpl {
    private final FloatArrayMemory arrayMemory;

    VmFloatArrayImpl(VmImpl vm, int size) {
        super(vm.floatArrayClass, MemoryFactory.wrap(new float[size]));
        arrayMemory = (FloatArrayMemory) ((CompositeMemory)getMemory()).getSubMemory(1);
    }

    VmFloatArrayImpl(final VmFloatArrayImpl original) {
        super(original);
        arrayMemory = (FloatArrayMemory) ((CompositeMemory)getMemory()).getSubMemory(1);
    }

    @Override
    public int getLength() {
        return arrayMemory.getArray().length;
    }

    @Override
    public float[] getArray() {
        return arrayMemory.getArray();
    }

    @Override
    protected VmFloatArrayImpl clone() {
        return new VmFloatArrayImpl(this);
    }
}
