package org.qbicc.interpreter.impl;

/**
 *
 */
final class VmRefArrayImpl extends VmArrayImpl {
    private final int size;

    VmRefArrayImpl(VmArrayClassImpl clazz, final int size) {
        super(clazz, size);
        this.size = size;
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
