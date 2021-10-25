package org.qbicc.interpreter.impl;

/**
 *
 */
final class VmBooleanArrayImpl extends VmArrayImpl {

    VmBooleanArrayImpl(VmImpl vm, int size) {
        super(vm.booleanArrayClass, size);
    }

    VmBooleanArrayImpl(final VmBooleanArrayImpl original) {
        super(original);
    }

    @Override
    public int getArrayElementOffset(int index) {
        return getVmClass().getVm().booleanArrayContentOffset + index;
    }

    @Override
    protected VmBooleanArrayImpl clone() {
        return new VmBooleanArrayImpl(this);
    }
}
