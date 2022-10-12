package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.VmArray;

/**
 *
 */
final class HooksForUnixNativeDispatcher {
    HooksForUnixNativeDispatcher() {}

    static VmArray getcwd(VmThreadImpl thread) {
        // TODO: this should return the default directory on the VFS
        return thread.vm.newByteArray(System.getProperty("user.dir").getBytes());
    }
}
