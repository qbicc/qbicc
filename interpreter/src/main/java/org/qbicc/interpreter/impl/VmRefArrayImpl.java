package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.VmReferenceArray;
import org.qbicc.type.ReferenceArrayObjectType;

/**
 *
 */
final class VmRefArrayImpl extends VmArrayImpl implements VmReferenceArray {
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
        VmImpl vm = getVmClass().getVm();
        int refSize = vm.getCompilationContext().getTypeSystem().getReferenceSize();
        return vm.refArrayContentOffset + index * refSize;
    }

    @Override
    public ReferenceArrayObjectType getObjectType() {
        return (ReferenceArrayObjectType) super.getObjectType();
    }
}
