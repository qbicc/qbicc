package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.Hook;
import org.qbicc.interpreter.VmArray;
import org.qbicc.interpreter.VmClass;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmThread;
import org.qbicc.type.ClassObjectType;
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

    @Hook
    static boolean isReferenceArray(VmThread thread, ValueType typeId) {
        return typeId instanceof ClassObjectType cot && cot.getDefinition().internalNameEquals("[L");
    }

    @Hook
    static int lengthOf(VmThread thread, VmArray array) {
        return array.getLength();
    }
}
