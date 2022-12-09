package org.qbicc.machine.llvm.impl;

import java.io.IOException;

final class OpaquePointer extends AbstractValue {
    private final int addrSpace;

    OpaquePointer(final int addrSpace) {
        this.addrSpace = addrSpace;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        target.append("ptr");
        if (addrSpace != 0) {
            target.append(' ').append("addrspace").append('(');
            target.append(Integer.toString(addrSpace));
            target.append(')');
        }
        return target;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof OpaquePointer op && equals(op);
    }

    boolean equals(OpaquePointer o) {
        return this == o || o != null && addrSpace == o.addrSpace;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(addrSpace);
    }
}
