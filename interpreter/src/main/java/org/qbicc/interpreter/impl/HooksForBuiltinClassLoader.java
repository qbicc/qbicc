package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.Hook;
import org.qbicc.interpreter.VmClass;
import org.qbicc.interpreter.VmString;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.element.MethodElement;

import java.util.List;

/**
 *
 */
final class HooksForBuiltinClassLoader {
    private final MethodElement emptyEnum;
    HooksForBuiltinClassLoader(VmImpl vm) {
        emptyEnum = vm.bootstrapClassLoader.loadClass("java/util/Collections").getTypeDefinition().requireSingleMethod("emptyEnumeration");
    }

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

    @Hook
    static VmObjectImpl findResourceOnClassPath(VmThreadImpl thread, VmClassLoaderImpl classLoader, VmString resourceName) {
        byte[] resource = classLoader.getClassContext().getResource(resourceName.getContent());
        if (resource != null) {
            // TODO: To handle this request, we would need to mount the resource in the VFS and then return a URL to it.
            thread.getVM().getCompilationContext().warning("TODO: Found resource %s on classpath, but returning null", resourceName.getContent());
        }
        return null;
    }

    @Hook
    Object findResourcesOnClassPath(VmThreadImpl thread, VmClassLoaderImpl classLoader, VmString resourceName) {
        List<byte[]> resources = classLoader.getClassContext().getResources(resourceName.getContent());
        if (!resources.isEmpty()) {
            // TODO: To handle this request, we would need to mount the resources in the VFS and then return an enumeration of the URLs to access them.
            thread.getVM().getCompilationContext().warning("TODO: Found %d resources %s on classpath, but returning null", resources.size(), resourceName.getContent());
        }
        return thread.getVM().invokeExact(emptyEnum, null, List.of());
    }
}
