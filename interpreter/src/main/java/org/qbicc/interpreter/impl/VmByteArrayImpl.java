package org.qbicc.interpreter.impl;

import java.util.Arrays;

import org.qbicc.interpreter.memory.ByteArrayMemory;
import org.qbicc.interpreter.memory.CompositeMemory;
import org.qbicc.interpreter.memory.MemoryFactory;

/**
 *
 */
final class VmByteArrayImpl extends VmArrayImpl {
    private final ByteArrayMemory arrayMemory;

    VmByteArrayImpl(VmImpl vm, byte[] orig) {
        super(vm.byteArrayClass, MemoryFactory.wrap(orig, vm.getCompilationContext().getTypeSystem().getEndianness()));
        arrayMemory = (ByteArrayMemory) ((CompositeMemory)getMemory()).getSubMemory(1);
    }

    VmByteArrayImpl(VmImpl vm, int size) {
        this(vm, new byte[size]);
    }

    VmByteArrayImpl(final VmByteArrayImpl original) {
        super(original);
        arrayMemory = (ByteArrayMemory) ((CompositeMemory)getMemory()).getSubMemory(1);
    }

    @Override
    public int getLength() {
        return arrayMemory.getArray().length;
    }

    @Override
    public byte[] getArray() {
        return arrayMemory.getArray();
    }

    @Override
    protected VmByteArrayImpl clone() {
        return new VmByteArrayImpl(this);
    }

    public VmByteArrayImpl copyOfRange(final int off, final int len) {
        return new VmByteArrayImpl(clazz.getVm(), Arrays.copyOfRange(getArray(), off, len));
    }
}
