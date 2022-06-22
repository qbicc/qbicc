package org.qbicc.plugin.apploader;

import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.atomic.AccessModes;
import org.qbicc.interpreter.Vm;
import org.qbicc.interpreter.VmClass;
import org.qbicc.interpreter.VmClassLoader;
import org.qbicc.type.definition.LoadedTypeDefinition;

import java.util.function.Consumer;

public class InitAppClassLoaderHook implements Consumer<CompilationContext> {
    private static final String CLASS_LOADERS = "jdk/internal/loader/ClassLoaders";

    public InitAppClassLoaderHook() {
    }

    public void accept(final CompilationContext ctxt) {
        ClassContext bootstrapClassContext = ctxt.getBootstrapClassContext();
        LoadedTypeDefinition classLoadersDef = bootstrapClassContext.findDefinedType(CLASS_LOADERS).load();
        VmClass classLoaders = classLoadersDef.getVmClass();
        try {
            Vm.requireCurrent().initialize(classLoaders);
        } catch (Throwable t) {
            ctxt.error("Failed to initialize %s: %s", CLASS_LOADERS, t);
            return;
        }
        VmClassLoader appClassLoader = (VmClassLoader) classLoaders.getStaticMemory().loadRef(classLoaders.indexOfStatic(classLoadersDef.findField("APP_LOADER")), AccessModes.SinglePlain);
        AppClassLoader.get(ctxt).setAppClassLoader(appClassLoader);
    }
}
