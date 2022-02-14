package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.memory.CharArrayMemory;
import org.qbicc.interpreter.memory.CompositeMemory;
import org.qbicc.interpreter.memory.MemoryFactory;

/**
 *
 */
final class VmCharArrayImpl extends VmArrayImpl {
    private final CharArrayMemory arrayMemory;

    VmCharArrayImpl(VmImpl vm, int size) {
        super(vm.charArrayClass, MemoryFactory.wrap(new char[size]));
        arrayMemory = (CharArrayMemory) ((CompositeMemory)getMemory()).getSubMemory(1);
    }

    VmCharArrayImpl(final VmCharArrayImpl original) {
        super(original);
        arrayMemory = (CharArrayMemory) ((CompositeMemory)getMemory()).getSubMemory(1);
    }

    @Override
    public int getLength() {
        return arrayMemory.getArray().length;
    }

    @Override
    public char[] getArray() {
        return arrayMemory.getArray();
    }

    @Override
    protected VmCharArrayImpl clone() {
        return new VmCharArrayImpl(this);
    }
}
