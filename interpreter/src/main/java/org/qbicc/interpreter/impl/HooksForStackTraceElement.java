package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.Hook;
import org.qbicc.interpreter.VmThread;

/**
 *
 */
final class HooksForStackTraceElement {
    HooksForStackTraceElement() {}

    @Hook
    static void initStackTraceElements(VmThread thread, VmArrayImpl stackTrace, VmThrowableImpl throwable) {
        throwable.initStackTraceElements(stackTrace);
    }
}
