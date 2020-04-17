package cc.quarkus.qcc.machine.llvm.impl;

import java.io.IOException;

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
}
