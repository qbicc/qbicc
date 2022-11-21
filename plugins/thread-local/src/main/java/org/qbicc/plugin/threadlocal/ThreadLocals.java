package org.qbicc.plugin.threadlocal;

import java.util.concurrent.ConcurrentHashMap;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.type.definition.element.InstanceFieldElement;
import org.qbicc.type.definition.element.StaticFieldElement;

/**
 *
 */
public final class ThreadLocals {
    private static final AttachmentKey<ThreadLocals> KEY = new AttachmentKey<>();

    private final CompilationContext ctxt;
    private final ConcurrentHashMap<StaticFieldElement, InstanceFieldElement> threadLocalFields = new ConcurrentHashMap<>();

    private ThreadLocals(final CompilationContext ctxt) {
        this.ctxt = ctxt;
    }

    public static ThreadLocals get(final CompilationContext ctxt) {
        ThreadLocals nativeInfo = ctxt.getAttachment(KEY);
        if (nativeInfo == null) {
            ThreadLocals appearing = ctxt.putAttachmentIfAbsent(KEY, nativeInfo = new ThreadLocals(ctxt));
            if (appearing != null) {
                nativeInfo = appearing;
            }
        }
        return nativeInfo;
    }

    public InstanceFieldElement getThreadLocalField(StaticFieldElement staticField) {
        return threadLocalFields.get(staticField);
    }

    void registerThreadLocalField(StaticFieldElement staticField, InstanceFieldElement injectedField) {
        threadLocalFields.put(staticField, injectedField);
    }
}
