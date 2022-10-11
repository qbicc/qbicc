package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.Hook;
import org.qbicc.interpreter.VmClass;
import org.qbicc.interpreter.VmString;
import org.qbicc.type.definition.DefinedTypeDefinition;

/**
 *
 */
final class HooksForBuiltinClassLoader {
    HooksForBuiltinClassLoader() {}

    @Hook
    static VmClass findClassOnClassPathOrNull(VmThreadImpl thread, VmClassLoaderImpl classLoader, VmString className) {
        VmString name = thread.vm.fixClassname(className);
        DefinedTypeDefinition definedType = classLoader.getClassContext().findDefinedType(name.getContent());
        if (definedType == null) {
            return null;
        }
        try {
            return definedType.load().getVmClass();
        } catch (Exception e) {
            return null;
        }
    }
}
