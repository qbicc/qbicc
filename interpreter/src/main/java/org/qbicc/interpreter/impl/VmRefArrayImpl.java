package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmReferenceArray;
import org.qbicc.interpreter.memory.CompositeMemory;
import org.qbicc.interpreter.memory.MemoryFactory;
import org.qbicc.interpreter.memory.ReferenceArrayMemory;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.ReferenceType;

/**
 *
 */
final class VmRefArrayImpl extends VmArrayImpl implements VmReferenceArray {
    private final ReferenceArrayMemory arrayMemory;

    VmRefArrayImpl(VmArrayClassImpl clazz, int size) {
        super(clazz, MemoryFactory.wrap(new VmObject[size], (ReferenceType) clazz.getInstanceObjectType().getElementType()));
        arrayMemory = (ReferenceArrayMemory) ((CompositeMemory)getMemory()).getSubMemory(1);
    }

    VmRefArrayImpl(final VmRefArrayImpl original) {
        super(original);
        arrayMemory = (ReferenceArrayMemory) ((CompositeMemory)getMemory()).getSubMemory(1);
    }

    @Override
    public int getLength() {
        return arrayMemory.getArray().length;
    }


    @Override
    public VmObject[] getArray() {
        return arrayMemory.getArray();
    }

    @Override
    public ReferenceArrayObjectType getObjectType() {
        return (ReferenceArrayObjectType) super.getObjectType();
    }

    @Override
    public void store(int index, VmObject value) {
        getArray()[index] = value;
    }

    @Override
    protected VmRefArrayImpl clone() {
        return new VmRefArrayImpl(this);
    }
}
