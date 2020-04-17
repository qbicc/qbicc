package cc.quarkus.qcc.machine.llvm.impl;

import java.io.IOException;

final class VoidReturn extends AbstractReturn {
    static final VoidReturn INSTANCE = new VoidReturn();

    private VoidReturn() {
        super();
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        return appendTrailer(super.appendTo(target).append(' ').append("void"));
    }
}
