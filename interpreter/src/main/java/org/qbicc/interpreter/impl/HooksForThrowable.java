package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.Hook;

/**
 *
 */
final class HooksForThrowable {
    HooksForThrowable() {}

    @Hook(descriptor = "(I)Ljava/lang/Throwable;")
    static VmThrowableImpl fillInStackTrace(VmThreadImpl thread, VmThrowableImpl target, int dummy) {
        target.fillInStackTrace();
        return target;
    }
}
