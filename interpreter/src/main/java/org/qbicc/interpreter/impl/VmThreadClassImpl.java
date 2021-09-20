package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.VmObject;
import org.qbicc.type.definition.LoadedTypeDefinition;

/**
 * A {@code Class} for a class which extends {@code Thread}.
 */
final class VmThreadClassImpl extends VmClassImpl {
    VmThreadClassImpl(final VmImpl vm, final LoadedTypeDefinition loaded, final VmObject protectionDomain) {
        super(vm, loaded, protectionDomain);
    }

    @Override
    VmThreadImpl newInstance() {
        return new VmThreadImpl(this, getVm());
    }
}
