package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.VmObject;
import org.qbicc.type.definition.LoadedTypeDefinition;

/**
 *
 */
final class VmClassLoaderClassImpl extends VmClassImpl {

    VmClassLoaderClassImpl(final VmImpl vm, final LoadedTypeDefinition loaded, final VmObject protectionDomain) {
        super(vm, loaded, protectionDomain);
    }

    @Override
    VmClassLoaderImpl newInstance() {
        return new VmClassLoaderImpl(this, getVm().getCompilationContext());
    }
}
