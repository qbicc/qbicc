package org.qbicc.interpreter.impl;


import org.qbicc.interpreter.Memory;
import org.qbicc.interpreter.VmArray;
import org.qbicc.type.ArrayObjectType;

/**
 *
 */
abstract class VmArrayImpl extends VmObjectImpl implements VmArray {

    VmArrayImpl(VmArrayClassImpl clazz, Memory arrayMemory) {
        super(clazz, arrayMemory);
    }

    VmArrayImpl(VmArrayImpl original) {
        super(original);
    }

    @Override
    public abstract int getLength();

    @Override
    public ArrayObjectType getObjectType() {
        return getVmClass().getInstanceObjectType();
    }

    @Override
    protected abstract VmArrayImpl clone();

    @Override
    public VmArrayClassImpl getVmClass() {
        return (VmArrayClassImpl) super.getVmClass();
    }

    StringBuilder toString(final StringBuilder target) {
        return target.append(getVmClass().getElementType().getName()).append('[').append(getLength()).append(']');
    }
}
