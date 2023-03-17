package org.qbicc.plugin.llvm;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.InvocationNode;
import org.qbicc.type.definition.LoadedTypeDefinition;

public final class LLVMInfo {
    private static final AttachmentKey<LLVMInfo> KEY = new AttachmentKey<>();

    private final CompilationContext ctxt;
    private final Map<LoadedTypeDefinition, List<InvocationNode>> statePointIds = new ConcurrentHashMap<>();

    private LLVMInfo(CompilationContext ctxt) {
        this.ctxt = ctxt;
    }

    public static LLVMInfo get(CompilationContext ctxt) {
        LLVMInfo info = ctxt.getAttachment(KEY);
        if (info == null) {
            info = new LLVMInfo(ctxt);
            final LLVMInfo appearing = ctxt.putAttachmentIfAbsent(KEY, info);
            if (appearing != null) {
                info = appearing;
            }
        }
        return info;
    }

    public List<InvocationNode> getStatePointIds(LoadedTypeDefinition def) {
        return statePointIds.get(def);
    }

    public void setStatePointIds(LoadedTypeDefinition def, List<InvocationNode> ids) {
        if (statePointIds.putIfAbsent(def, ids) != null) {
            throw new IllegalStateException("State point IDs set twice");
        }
    }
}
