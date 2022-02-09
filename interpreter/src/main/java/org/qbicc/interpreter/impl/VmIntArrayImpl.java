package org.qbicc.interpreter.impl;

/**
 *
 */
final class VmIntArrayImpl extends VmArrayImpl {

    VmIntArrayImpl(VmImpl vm, int size) {
        super(vm.intArrayClass, size);
    }

    VmIntArrayImpl(final VmIntArrayImpl original) {
        super(original);
    }

    @Override
    public long getArrayElementOffset(int index) {
        return getVmClass().getVm().intArrayContentOffset + ((long) index << 2);
    }

    @Override
    protected VmIntArrayImpl clone() {
        return new VmIntArrayImpl(this);
    }
}
