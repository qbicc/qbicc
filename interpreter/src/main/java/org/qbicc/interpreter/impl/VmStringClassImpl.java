package org.qbicc.interpreter.impl;

import org.qbicc.type.definition.LoadedTypeDefinition;

/**
 * The {@code VmClass} for {@code String.class}.
 */
final class VmStringClassImpl extends VmClassImpl {

    VmStringClassImpl(VmImpl vmImpl, LoadedTypeDefinition typeDefinition) {
        super(vmImpl, typeDefinition, null);
    }

    @Override
    VmStringImpl newInstance() {
        return new VmStringImpl(this);
    }
}
