package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.Hook;
import org.qbicc.interpreter.VmClass;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmThread;

/**
 *
 */
final class HooksForMethodHandlesLookup {
    HooksForMethodHandlesLookup() {}

    // TODO: temporary workaround for var/method handle initialization
    @Hook
    static void checkAccess(VmThread thread, VmObject lookup, byte refKind, VmClass refClass, VmObject m) {
        // Make access check methods always return true
    }

    // TODO: temporary workaround for var/method handle initialization
    @Hook
    static void checkMethod(VmThread thread, VmObject lookup, byte refKind, VmClass refClass, VmObject m) {
        // Make access check methods always return true
    }

    // TODO: temporary workaround for var/method handle initialization
    @Hook
    static void checkField(VmThread thread, VmObject lookup, byte refKind, VmClass refClass, VmObject m) {
        // Make access check methods always return true
    }
}
