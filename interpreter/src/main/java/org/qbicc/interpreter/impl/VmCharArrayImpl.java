package org.qbicc.interpreter.impl;

/**
 *
 */
final class VmCharArrayImpl extends VmArrayImpl {

    VmCharArrayImpl(VmImpl vm, int size) {
        super(vm.charArrayClass, size);
    }

    @Override
    public int getArrayElementOffset(int index) {
        return getVmClass().getVm().charArrayContentOffset + index;
    }
}
