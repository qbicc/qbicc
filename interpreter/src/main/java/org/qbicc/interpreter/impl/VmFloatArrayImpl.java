package org.qbicc.interpreter.impl;

/**
 *
 */
final class VmFloatArrayImpl extends VmArrayImpl {

    VmFloatArrayImpl(VmImpl vm, int size) {
        super(vm.floatArrayClass, size);
    }

    @Override
    public int getArrayElementOffset(int index) {
        return getVmClass().getVm().floatArrayContentOffset + (index << 2);
    }
}
