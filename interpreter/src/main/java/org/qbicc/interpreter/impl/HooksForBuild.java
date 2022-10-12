package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.Hook;
import org.qbicc.interpreter.VmThread;

/**
 *
 */
final class HooksForBuild {
    HooksForBuild() {}

    @Hook
    static boolean isHost(VmThread thread) {
        return true;
    }

    @Hook
    static boolean isTarget(VmThread thread) {
        return false;
    }
}
