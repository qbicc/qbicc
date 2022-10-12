package org.qbicc.interpreter.impl;

import static org.qbicc.graph.atomic.AccessModes.SinglePlain;

import org.qbicc.interpreter.Hook;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmReferenceArray;
import org.qbicc.interpreter.VmString;
import org.qbicc.interpreter.VmThread;
import org.qbicc.type.definition.element.FieldElement;

/**
 *
 */
final class HooksForModule {

    private final FieldElement moduleLoaderField;

    HooksForModule(VmImpl vm) {
        moduleLoaderField = vm.bootstrapClassLoader.loadClass("java/lang/Module").getTypeDefinition().findField("loader", true);
    }

    @Hook
    void defineModule0(VmThreadImpl thread, VmObjectImpl module, boolean isOpen, VmString versionObj, VmString locationObj, VmReferenceArray packageNames) {
        VmClassLoaderImpl loader = (VmClassLoaderImpl) module.getMemory().loadRef(module.getVmClass().indexOf(moduleLoaderField), SinglePlain);
        if (loader == null) {
            loader = thread.vm.bootstrapClassLoader;
        }
        for (VmObject vmObject : packageNames.getArray()) {
            VmStringImpl packageNameObj = (VmStringImpl) vmObject;
            String packageName = packageNameObj.getContent();
            loader.setModulePackage(packageName, module);
        }
    }

    @Hook
    static void addReads0(VmThread thread, VmObject from, VmObject to) {}

    @Hook
    static void addExports0(VmThread thread, VmObject from, VmString pn, VmObject to) {}

    @Hook
    static void addExportsToAll0(VmThread thread, VmObject from, VmString pn) {}

    @Hook
    static void addExportsToAllUnnamed0(VmThread thread, VmObject from, VmString pn) {}
}
