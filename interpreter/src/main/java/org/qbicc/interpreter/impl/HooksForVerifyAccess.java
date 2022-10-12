package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.Hook;
import org.qbicc.interpreter.VmClass;
import org.qbicc.interpreter.VmThread;

/**
 *
 */
final class HooksForVerifyAccess {
    HooksForVerifyAccess() {}

    // TODO: temporary workaround for var/method handle initialization
    @Hook
    static boolean isClassAccessible(VmThread thread, VmClass refClass, VmClass lookupClass, VmClass prevLookupClass, int allowedModes) {
        // Make access check methods always return true
        return true;
    }
}
