package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.Hook;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmThread;

/**
 *
 */
final class HooksForRuntime {
    HooksForRuntime() {}

    @Hook
    static int availableProcessors(VmThread thread, VmObject runtime) {
        return Runtime.getRuntime().availableProcessors();
    }
}
