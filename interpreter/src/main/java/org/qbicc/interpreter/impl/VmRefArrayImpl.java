package org.qbicc.interpreter.impl;

import org.qbicc.type.ReferenceArrayObjectType;

/**
 *
 */
final class VmRefArrayImpl extends VmArrayImpl {
    private final ReferenceArrayObjectType arrayType;
    private final int size;

    VmRefArrayImpl(VmArrayClassImpl clazz, final ReferenceArrayObjectType arrayType, final int size) {
        super(clazz, size);
        this.arrayType = arrayType;
        this.size = size;
    }

    @Override
    public ReferenceArrayObjectType getObjectType() {
        return arrayType;
    }

    @Override
    public int getLength() {
        return size;
    }

    @Override
    public int getArrayElementOffset(int index) {
        return index;
    }
}
