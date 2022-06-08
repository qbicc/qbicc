package org.qbicc.machine.llvm.impl;

import static org.qbicc.machine.arch.AddressSpaceConstants.DEFAULT;

import java.io.IOException;
import java.util.Objects;

final class PointerTo extends AbstractValue {
    private final AbstractValue type;
    private final int addrSpace;

    PointerTo(final AbstractValue type, final int addrSpace) {
        this.type = type;
        this.addrSpace = addrSpace;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        type.appendTo(target);
        if (addrSpace != DEFAULT) {
            target.append(' ').append("addrspace").append('(');
            target.append(Integer.toString(addrSpace));
            target.append(')');
        }
        return target.append('*');
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PointerTo pointerTo = (PointerTo) o;
        return addrSpace == pointerTo.addrSpace && type.equals(pointerTo.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, addrSpace);
    }
}
