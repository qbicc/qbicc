package org.qbicc.machine.llvm.impl;

import java.io.IOException;

final class FunctionDeclarationImpl extends AbstractFunction {
    FunctionDeclarationImpl(final String name) {
        super(name);
    }

    @Override
    public Appendable appendTo(Appendable target) throws IOException {
        target.append("declare ");
        appendLinkage(target);
        appendVisibility(target);
        appendDllStorageClass(target);
        appendCallingConvention(target);
        appendNameAndType(target);
        appendAddressNaming(target);
        appendAddressSpace(target);
        appendFunctionAttributes(target);
        appendAlign(target);

        return target;
    }
}
