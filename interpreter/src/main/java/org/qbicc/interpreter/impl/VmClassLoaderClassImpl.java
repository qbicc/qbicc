package org.qbicc.interpreter.impl;

import org.qbicc.type.definition.LoadedTypeDefinition;

/**
 *
 */
final class VmClassLoaderClassImpl extends VmClassImpl {

    VmClassLoaderClassImpl(final VmImpl vm, final LoadedTypeDefinition loaded) {
        super(vm, loaded);
    }

    @Override
    VmClassLoaderImpl newInstance() {
        return new VmClassLoaderImpl(this, getVm().getCompilationContext());
    }
}
