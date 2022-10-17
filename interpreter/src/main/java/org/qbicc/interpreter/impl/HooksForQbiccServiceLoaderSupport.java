package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.Hook;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmReferenceArray;
import org.qbicc.plugin.reachability.ServiceLoaderAnalyzer;
import org.qbicc.type.definition.LoadedTypeDefinition;

import java.util.List;

public class HooksForQbiccServiceLoaderSupport {
    HooksForQbiccServiceLoaderSupport() {
    }

    @Hook
    VmReferenceArray findProviders(VmThreadImpl thread, VmClassImpl service) {
        VmImpl vm = thread.getVM();
        List<LoadedTypeDefinition> providers = ServiceLoaderAnalyzer.get(vm.getCompilationContext()).getProviders(service.getTypeDefinition());
        if (providers.isEmpty()) {
            return null;
        } else {
            VmReferenceArray array = vm.newArrayOf(vm.classClass, providers.size()+1);
            VmObject[] arrayArray = array.getArray();
            arrayArray[0] = service;
            int cursor = 1;
            for (LoadedTypeDefinition p: providers) {
                arrayArray[cursor++] = p.getVmClass();
            }
            return array;
        }
    }
}
