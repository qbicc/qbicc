package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.Hook;
import org.qbicc.interpreter.VmClass;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmString;
import org.qbicc.machine.vfs.AbsoluteVirtualPath;
import org.qbicc.machine.vfs.VirtualFileSystem;
import org.qbicc.plugin.vfs.VFS;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.MethodElement;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
final class HooksForBuiltinClassLoader {
    private final MethodElement emptyEnum;
    private final MethodElement enumeration;
    private final VmClass urlCLass;
    private final ConstructorElement urlConstructor;
    private final VmClass arrayListClass;
    private final ConstructorElement arrayListConstructor;
    private final MethodElement arrayListAdd;


    HooksForBuiltinClassLoader(VmImpl vm) {
        LoadedTypeDefinition collections = vm.bootstrapClassLoader.loadClass("java/util/Collections").getTypeDefinition();
        emptyEnum = collections.requireSingleMethod("emptyEnumeration");
        enumeration = collections.requireSingleMethod("enumeration");
        urlCLass = vm.bootstrapClassLoader.loadClass("java/net/URL");
        urlConstructor = urlCLass.getTypeDefinition().requireSingleConstructor(ce -> ce.getParameters().size() == 1);
        arrayListClass = vm.bootstrapClassLoader.loadClass("java/util/ArrayList");
        arrayListConstructor = arrayListClass.getTypeDefinition().requireSingleConstructor(ce -> ce.getParameters().isEmpty());
        arrayListAdd = arrayListClass.getTypeDefinition().requireSingleMethod("add", 1);
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

    private VmObject mountResource(VmThreadImpl thread, VmClassLoaderImpl classLoader, VmString resourceName, int index, byte[] resource) {
        VFS vfs = VFS.get(classLoader.getClassContext().getCompilationContext());
        VirtualFileSystem fileSystem = vfs.getFileSystem();
        try {
            AbsoluteVirtualPath resourceDir = vfs.getQbiccPath().resolve("classpath/hostjvm/" + classLoader.getName()+"/"+index);
            fileSystem.mkdirs(resourceDir, 0777);
            AbsoluteVirtualPath resourcePath = resourceDir.resolve(resourceName.getContent());
            fileSystem.bindHostBytes(resourcePath, resource, true);
            String fn = resourcePath.toString();
            String url = "file://"+fn;
            return thread.getVM().newInstance(urlCLass, urlConstructor, List.of(url));
        } catch (IOException e) {
            thread.getVM().getCompilationContext().warning(e, "IOException mounting %s into VFS", resourceName.getContent());
            return null;
        }
    }

    @Hook
    VmObject findResourceOnClassPath(VmThreadImpl thread, VmClassLoaderImpl classLoader, VmString resourceName) {
        byte[] resource = classLoader.getClassContext().getResource(resourceName.getContent());
        if (resource != null) {
            return mountResource(thread, classLoader, resourceName,0, resource);
        } else {
            return null;
        }
    }

    @Hook
    Object findResourcesOnClassPath(VmThreadImpl thread, VmClassLoaderImpl classLoader, VmString resourceName) {
        List<byte[]> resources = classLoader.getClassContext().getResources(resourceName.getContent());
        if (!resources.isEmpty()) {
            VmObject results = thread.getVM().newInstance(arrayListClass, arrayListConstructor, List.of());
            int index = 0;
            for (byte[] resource: resources) {
                VmObject url = mountResource(thread, classLoader, resourceName, index++, resource);
                thread.getVM().invokeExact(arrayListAdd, results, List.of(url));
            }
            Object theEnum = thread.getVM().invokeExact(enumeration, null, List.of(results));
            return theEnum;
         }
        return thread.getVM().invokeExact(emptyEnum, null, List.of());
    }
}
