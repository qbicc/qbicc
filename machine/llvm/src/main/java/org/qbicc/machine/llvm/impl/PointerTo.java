package org.qbicc.machine.llvm.impl;

import java.io.IOException;

final class PointerTo extends AbstractValue {
    private final AbstractValue type;
    private final int addrSpace;

    PointerTo(final AbstractValue type, final int addrSpace) {
        this.type = type;
        this.addrSpace = addrSpace;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        type.appendTo(target);
        if (addrSpace != 0) {
            target.append(' ').append("addrSpace").append('(');
            target.append(Integer.toString(addrSpace));
            target.append(')');
        }
        return target.append('*');
    }
}
