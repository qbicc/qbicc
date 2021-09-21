package org.qbicc.interpreter.impl;

/**
 *
 */
final class VmDoubleArrayImpl extends VmArrayImpl {

    VmDoubleArrayImpl(VmImpl vm, int size) {
        super(vm.doubleArrayClass, size);
    }

    @Override
    public int getArrayElementOffset(int index) {
        return getVmClass().getVm().doubleArrayContentOffset + (index << 3);
    }
}
