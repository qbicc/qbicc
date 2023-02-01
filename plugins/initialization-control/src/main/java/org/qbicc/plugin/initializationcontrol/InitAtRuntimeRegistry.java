package org.qbicc.plugin.initializationcontrol;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InitAtRuntimeRegistry {

    private static final AttachmentKey<InitAtRuntimeRegistry> KEY = new AttachmentKey<>();

    private final CompilationContext ctxt;
    private final Set<String> runtimeInitializedClasses = ConcurrentHashMap.newKeySet();

    private InitAtRuntimeRegistry(CompilationContext ctxt) {
        this.ctxt = ctxt;
    }

    public static InitAtRuntimeRegistry get(CompilationContext ctxt) {
        InitAtRuntimeRegistry patcher = ctxt.getAttachment(KEY);
        if (patcher == null) {
            patcher = new InitAtRuntimeRegistry(ctxt);
            InitAtRuntimeRegistry appearing = ctxt.putAttachmentIfAbsent(KEY, patcher);
            if (appearing != null) {
                patcher = appearing;
            }
        }
        return patcher;
    }

    public void addRuntimeInitializedClass(String internalName) {
        runtimeInitializedClasses.add(internalName);
    }

    public boolean isRuntimeInitializedClass(String internalName) {
        return runtimeInitializedClasses.contains(internalName);
    }
}
