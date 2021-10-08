package org.qbicc.interpreter.impl;


import org.qbicc.interpreter.VmArray;
import org.qbicc.type.ArrayObjectType;

/**
 *
 */
abstract class VmArrayImpl extends VmObjectImpl implements VmArray {
    private final int length;

    VmArrayImpl(VmArrayClassImpl clazz, int size) {
        super(clazz, size);
        this.length = size;
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public ArrayObjectType getObjectType() {
        return (ArrayObjectType) getVmClass().getInstanceObjectType();
    }
}
