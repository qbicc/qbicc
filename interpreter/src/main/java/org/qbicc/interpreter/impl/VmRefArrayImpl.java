package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.VmReferenceArray;
import org.qbicc.type.ReferenceArrayObjectType;

/**
 *
 */
final class VmRefArrayImpl extends VmArrayImpl implements VmReferenceArray {

    VmRefArrayImpl(VmArrayClassImpl clazz, final int size) {
        super(clazz, size);
    }

    VmRefArrayImpl(final VmRefArrayImpl original) {
        super(original);
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

    @Override
    protected VmRefArrayImpl clone() {
        return new VmRefArrayImpl(this);
    }
}
