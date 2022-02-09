package org.qbicc.interpreter.impl;

/**
 *
 */
final class VmFloatArrayImpl extends VmArrayImpl {

    VmFloatArrayImpl(VmImpl vm, int size) {
        super(vm.floatArrayClass, size);
    }

    VmFloatArrayImpl(final VmFloatArrayImpl original) {
        super(original);
    }

    @Override
    public long getArrayElementOffset(int index) {
        return getVmClass().getVm().floatArrayContentOffset + ((long) index << 2);
    }

    @Override
    protected VmFloatArrayImpl clone() {
        return new VmFloatArrayImpl(this);
    }
}
