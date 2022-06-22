package org.qbicc.plugin.apploader;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.interpreter.VmClassLoader;

/**
 * The AppClassLoader attachment.
 */
public final class AppClassLoader {
    private static final AttachmentKey<AppClassLoader> KEY = new AttachmentKey<>();

    private VmClassLoader appClassLoader;

    private AppClassLoader() {}

    public static AppClassLoader get(CompilationContext ctxt) {
        return ctxt.computeAttachmentIfAbsent(KEY, AppClassLoader::new);
    }

    public VmClassLoader getAppClassLoader() {
        return appClassLoader;
    }

    public void setAppClassLoader(final VmClassLoader cl) {
        this.appClassLoader = cl;
    }
}