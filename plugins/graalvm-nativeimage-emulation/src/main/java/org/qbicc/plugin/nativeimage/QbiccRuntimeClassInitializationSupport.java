package org.qbicc.plugin.nativeimage;

import org.graalvm.nativeimage.impl.RuntimeClassInitializationSupport;
import org.qbicc.context.CompilationContext;

public class QbiccRuntimeClassInitializationSupport implements RuntimeClassInitializationSupport {
    private final CompilationContext ctxt;

    QbiccRuntimeClassInitializationSupport(CompilationContext ctxt) {
        this.ctxt = ctxt;
    }

    @Override
    public void initializeAtRunTime(String name, String reason) {
        ctxt.warning("ignoring: initializeAtRuntime %s %s", name, reason);
    }

    @Override
    public void initializeAtBuildTime(String name, String reason) {
        // No-op for qbicc; this is what we do anyways
    }

    @Override
    public void rerunInitialization(String name, String reason) {
        ctxt.warning("ignoring: rerunInitialization %s %s", name, reason);
    }

    @Override
    public void initializeAtRunTime(Class<?> aClass, String reason) {
        ctxt.warning("ignoring: initializeAtRuntime %s %s", aClass.toString(), reason);
    }

    @Override
    public void rerunInitialization(Class<?> aClass, String reason) {
        ctxt.warning("ignoring: rerunInitialization %s %s", aClass.toString(), reason);
    }

    @Override
    public void initializeAtBuildTime(Class<?> aClass, String reason) {
        // No-op for qbicc; this is what we do anyways
    }

    @Override
    public void reportClassInitialized(Class<?> aClass, StackTraceElement[] stackTrace) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reportObjectInstantiated(Object o, StackTraceElement[] stackTrace) {
        throw new UnsupportedOperationException();
    }
}
