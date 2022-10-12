package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.Hook;
import org.qbicc.interpreter.VmClass;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmThread;

/**
 *
 */
final class HooksForVMHelpers {
    HooksForVMHelpers() {}

    @Hook
    static VmClass getClassFromObject(VmThread thread, VmObject obj) {
        return obj.getVmClass();
    }
}
