package org.qbicc.interpreter.impl;

/**
 *
 */
final class VmByteArrayImpl extends VmArrayImpl {

    VmByteArrayImpl(VmImpl vm, int size) {
        super(vm.byteArrayClass, size);
    }

    VmByteArrayImpl(final VmImpl vm, final byte[] bytes, int offs, int len) {
        this(vm, bytes.length);
        getMemory().storeMemory(getArrayElementOffset(0), bytes, offs, len);
    }

    VmByteArrayImpl(final VmImpl vm, final byte[] bytes) {
        this(vm, bytes, 0, bytes.length);
    }

    VmByteArrayImpl(VmByteArrayImpl original) {
        super(original);
    }

    @Override
    public long getArrayElementOffset(int index) {
        return getVmClass().getVm().byteArrayContentOffset + index;
    }

    @Override
    protected VmByteArrayImpl clone() {
        return new VmByteArrayImpl(this);
    }
}
