package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.Hook;
import org.qbicc.interpreter.VmThread;

public class HooksForBits {
    @Hook
    static void reserveMemory(VmThread thread, long size, long cap) {
        // no-op at build time
    }
}
