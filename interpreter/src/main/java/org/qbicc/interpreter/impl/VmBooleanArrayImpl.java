package org.qbicc.interpreter.impl;

/**
 *
 */
final class VmBooleanArrayImpl extends VmArrayImpl {

    VmBooleanArrayImpl(VmImpl vm, int size) {
        super(vm.booleanArrayClass, size);
    }

    @Override
    public int getArrayElementOffset(int index) {
        return getVmClass().getVm().booleanArrayContentOffset + index;
    }
}
