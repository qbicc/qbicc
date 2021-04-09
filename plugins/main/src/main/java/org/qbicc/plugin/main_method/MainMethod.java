package org.qbicc.plugin.main_method;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;

/**
 * The main method attachment.
 */
public final class MainMethod {
    private static final AttachmentKey<MainMethod> KEY = new AttachmentKey<>();

    private String mainClass;

    private MainMethod() {}

    public static MainMethod get(CompilationContext ctxt) {
        return ctxt.computeAttachmentIfAbsent(KEY, MainMethod::new);
    }

    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(final String mainClass) {
        this.mainClass = mainClass;
    }
}
