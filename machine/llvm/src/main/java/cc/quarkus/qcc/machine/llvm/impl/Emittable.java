package cc.quarkus.qcc.machine.llvm.impl;

import java.io.IOException;

/**
 *
 */
interface Emittable {
    Appendable appendTo(Appendable target) throws IOException;
}
