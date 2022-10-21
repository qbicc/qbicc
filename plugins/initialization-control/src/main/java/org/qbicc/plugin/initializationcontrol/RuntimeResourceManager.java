package org.qbicc.plugin.initializationcontrol;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.interpreter.Vm;
import org.qbicc.interpreter.VmArray;
import org.qbicc.interpreter.VmClassLoader;
import org.qbicc.interpreter.VmObject;
import org.qbicc.plugin.apploader.AppClassLoader;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.MethodElement;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RuntimeResourceManager {
    private static final AttachmentKey<RuntimeResourceManager> KEY = new AttachmentKey<>();

    private final CompilationContext ctxt;
    private final Set<String> runtimeResource = ConcurrentHashMap.newKeySet();
    private final Set<String> runtimeResources = ConcurrentHashMap.newKeySet();

    RuntimeResourceManager(CompilationContext ctxt) {
        this.ctxt = ctxt;
    }

    public static RuntimeResourceManager get(CompilationContext ctxt) {
        RuntimeResourceManager patcher = ctxt.getAttachment(KEY);
        if (patcher == null) {
            patcher = new RuntimeResourceManager(ctxt);
            RuntimeResourceManager appearing = ctxt.putAttachmentIfAbsent(KEY, patcher);
            if (appearing != null) {
                patcher = appearing;
            }
        }
        return patcher;
    }

    public void addResource(String name) {
        runtimeResource.add(name);
    }

    public void addResources(String name) {
        runtimeResources.add(name);
    }

    public void findAndSerializeResources() {
        if (runtimeResource.isEmpty() && runtimeResources.isEmpty()) {
            return;
        }
        VmClassLoader appCl = AppClassLoader.get(ctxt).getAppClassLoader();
        ClassContext appContext = appCl.getClassContext();
        LoadedTypeDefinition nir = appContext.findDefinedType("jdk/internal/loader/NativeImageResources").load();
        MethodElement addResource = nir.requireSingleMethod("addResource");
        Vm vm = appContext.getCompilationContext().getVm();
        for (String name: runtimeResource) {
            byte[] bytes = appContext.getResource(name);
            if (bytes != null) {
                vm.invokeExact(addResource, null, List.of(appCl, name, vm.newByteArray(bytes)));
            }
        }
        for (String name: runtimeResources) {
            List<byte[]> bytes = appContext.getResources(name);
            for (byte[] b: bytes) {
                vm.invokeExact(addResource, null, List.of(appCl, name, vm.newByteArray(b)));
            }
        }
    }
}
