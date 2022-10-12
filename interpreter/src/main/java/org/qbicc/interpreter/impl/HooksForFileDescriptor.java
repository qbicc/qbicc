package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.Hook;
import org.qbicc.interpreter.VmThread;

/**
 *
 */
final class HooksForFileDescriptor {
    HooksForFileDescriptor() {}

    @Hook
    static boolean getAppend(VmThread thread, int fd) {
        return false;
    }

    @Hook
    static long getHandle(VmThread thread, int fd) {
        return -1;
    }
}
