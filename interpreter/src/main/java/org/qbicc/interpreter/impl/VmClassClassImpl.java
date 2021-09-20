package org.qbicc.interpreter.impl;

/**
 * Singleton {@code VmClass} for {@code Class.class}.
 */
final class VmClassClassImpl extends VmClassImpl {
    VmClassClassImpl(VmImpl vm) {
        super(vm, vm.getCompilationContext().getBootstrapClassContext(), VmClassClassImpl.class);
    }

    @Override
    VmObjectImpl newInstance() {
        throw new UnsupportedOperationException("Cannot construct class objects");
    }
}
