package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.Hook;
import org.qbicc.interpreter.VmClass;
import org.qbicc.interpreter.VmThread;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.classfile.ClassFile;

/**
 *
 */
final class HooksForReflection {
    HooksForReflection() {}

    @Hook
    static VmClass getCallerClass(VmThreadImpl thread) {
        Frame currentFrame = thread.currentFrame;
        Frame enclosing = currentFrame.enclosing;
        while (enclosing.element.getEnclosingType().getInternalName().equals("java/lang/reflect/Method") || enclosing.element.hasAllModifiersOf(ClassFile.I_ACC_HIDDEN)) {
            enclosing = enclosing.enclosing;
        }
        DefinedTypeDefinition def = enclosing.element.getEnclosingType();
        return def.load().getVmClass();
    }

    @Hook
    static int getClassAccessFlags(VmThread thread, VmClass clazz) {
        return clazz.getTypeDefinition().getModifiers() & 0x1FFF;
    }
}
