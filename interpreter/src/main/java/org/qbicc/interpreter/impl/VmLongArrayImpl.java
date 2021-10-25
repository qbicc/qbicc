package org.qbicc.interpreter.impl;

/**
 *
 */
final class VmLongArrayImpl extends VmArrayImpl {

    VmLongArrayImpl(VmImpl vm, int size) {
        super(vm.longArrayClass, size);
    }

    VmLongArrayImpl(final VmLongArrayImpl original) {
        super(original);
    }

    @Override
    public int getArrayElementOffset(int index) {
        return getVmClass().getVm().longArrayContentOffset + (index << 3);
    }

    @Override
    protected VmLongArrayImpl clone() {
        return new VmLongArrayImpl(this);
    }
}
