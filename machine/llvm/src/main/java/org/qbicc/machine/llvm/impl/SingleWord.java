package org.qbicc.machine.llvm.impl;

import java.io.IOException;
import java.util.Objects;

final class SingleWord extends AbstractValue {
    private final String name;

    SingleWord(final String name) {
        this.name = name;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        return target.append(name);
    }

    public String toString() {
        // optimized
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SingleWord that = (SingleWord) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
