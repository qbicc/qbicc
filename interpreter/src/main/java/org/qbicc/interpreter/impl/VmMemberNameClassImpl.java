package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.VmObject;
import org.qbicc.type.definition.LoadedTypeDefinition;

/**
 *
 */
final class VmMemberNameClassImpl extends VmClassImpl {
    VmMemberNameClassImpl(VmImpl vmImpl, LoadedTypeDefinition typeDefinition, VmObject protectionDomain) {
        super(vmImpl, typeDefinition, protectionDomain);
    }

    @Override
    VmMemberNameImpl newInstance() {
        return new VmMemberNameImpl(this);
    }
}
