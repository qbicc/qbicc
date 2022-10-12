package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.Hook;
import org.qbicc.interpreter.VmThread;

/**
 *
 */
final class HooksForOSEnvironment {
    HooksForOSEnvironment() {}

    @Hook
    static void initialize(VmThread thread) {}
}
