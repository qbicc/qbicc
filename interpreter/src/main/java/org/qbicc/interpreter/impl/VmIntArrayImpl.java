package org.qbicc.interpreter.impl;

/**
 *
 */
final class VmIntArrayImpl extends VmArrayImpl {

    VmIntArrayImpl(VmImpl vm, int size) {
        super(vm.intArrayClass, size);
    }

    @Override
    public int getArrayElementOffset(int index) {
        return getVmClass().getVm().intArrayContentOffset + index;
    }
}
