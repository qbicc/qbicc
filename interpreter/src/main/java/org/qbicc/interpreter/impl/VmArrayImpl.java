package org.qbicc.interpreter.impl;


import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.interpreter.VmArray;

/**
 *
 */
abstract class VmArrayImpl extends VmObjectImpl implements VmArray {

    VmArrayImpl(VmArrayClassImpl clazz, int size) {
        super(clazz, size);
        // rely on post-construct fence
        VmImpl vm = clazz.getVm();
        getMemory().store32(vm.arrayLengthOffset, size, MemoryAtomicityMode.UNORDERED);
    }

    @Override
    public int getLength() {
        VmImpl vm = VmImpl.require();
        return getMemory().load32(vm.arrayLengthOffset, MemoryAtomicityMode.UNORDERED);
    }
}
