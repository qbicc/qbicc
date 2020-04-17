package cc.quarkus.qcc.machine.llvm.impl;

import java.io.IOException;

final class NamedLocalValueOf extends AbstractValue {
    private final String name;

    NamedLocalValueOf(final String name) {
        super();
        this.name = name;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        return target.append('%').append(name);
    }
}
