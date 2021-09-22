package org.qbicc.interpreter.impl;

/**
 *
 */
final class VmRefArrayImpl extends VmArrayImpl {
    private final int size;

    VmRefArrayImpl(VmArrayClassImpl clazz, final int size) {
        super(clazz, size);
        this.size = size;
    }

    @Override
    public int getLength() {
        return size;
    }

    @Override
    public int getArrayElementOffset(int index) {
        VmImpl vm = getVmClass().getVm();
        int refSize = vm.getCompilationContext().getTypeSystem().getReferenceSize();
        return vm.refArrayContentOffset + index * refSize;
    }
}
