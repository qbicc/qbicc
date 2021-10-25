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
    public int getArrayElementOffset(int index) {
        return getVmClass().getVm().intArrayContentOffset + (index << 2);
    }

    @Override
    protected VmIntArrayImpl clone() {
        return new VmIntArrayImpl(this);
    }
}
