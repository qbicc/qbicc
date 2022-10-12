package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.Hook;
import org.qbicc.interpreter.VmClass;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmThread;
import org.qbicc.type.ValueType;

/**
 *
 */
final class HooksForCompilerIntrinsics {
    HooksForCompilerIntrinsics() {}

    @Hook
    static ValueType typeIdOf(VmThread thread, VmObject object) {
        return object.getObjectTypeId();
    }

    @Hook
    static ValueType getTypeIdFromClass(VmThread thread, VmClass clazz) {
        return clazz.getInstanceObjectTypeId();
    }
}
