package org.qbicc.plugin.nativeimage;

import org.graalvm.nativeimage.impl.RuntimeClassInitializationSupport;
import org.qbicc.context.CompilationContext;
import org.qbicc.plugin.initializationcontrol.FeaturePatcher;

public class QbiccRuntimeClassInitializationSupport implements RuntimeClassInitializationSupport {
    private final CompilationContext ctxt;

    QbiccRuntimeClassInitializationSupport(CompilationContext ctxt) {
        this.ctxt = ctxt;
    }

    @Override
    public void initializeAtRunTime(String name, String reason) {
        String internalName = name.replace('.', '/');
        FeaturePatcher.get(ctxt).addRuntimeInitializedClass(internalName);
    }

    @Override
    public void initializeAtBuildTime(String name, String reason) {
        // No-op for qbicc; this is what we do by default
    }

    @Override
    public void rerunInitialization(String name, String reason) {
        ctxt.warning("ignoring: rerunInitialization %s %s", name, reason);
    }

    @Override
    public void initializeAtRunTime(Class<?> aClass, String reason) {
        String name = aClass.getName();
        String internalName = name.replace('.', '/');
        FeaturePatcher.get(ctxt).addRuntimeInitializedClass(internalName);
    }

    @Override
    public void rerunInitialization(Class<?> aClass, String reason) {
        ctxt.warning("ignoring: rerunInitialization %s %s", aClass.toString(), reason);
    }

    @Override
    public void initializeAtBuildTime(Class<?> aClass, String reason) {
        // No-op for qbicc; this is what we do by default
    }
}
