package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.Hook;
import org.qbicc.interpreter.VmArray;
import org.qbicc.interpreter.VmClass;

/**
 *
 */
final class HooksForArray {
    HooksForArray() {}

    @Hook
    static VmArray newArray(VmThreadImpl thread, VmClass componentType, int length) {
        return thread.vm.manuallyInitialize(componentType.getArrayClass().newInstance(length));
    }
}
