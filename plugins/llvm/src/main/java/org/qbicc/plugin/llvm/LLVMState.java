package org.qbicc.plugin.llvm;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.InvocationNode;
import org.qbicc.object.ProgramModule;
import org.qbicc.type.definition.LoadedTypeDefinition;

/**
 *
 */
final class LLVMState {
    private static final AttachmentKey<LLVMState> KEY = new AttachmentKey<>();

    private final Map<LoadedTypeDefinition, Path> pathsByType = new ConcurrentHashMap<>();
    private final ConcurrentMap<ProgramModule, List<List<InvocationNode>>> statePointInvocations = new ConcurrentHashMap<>();

    LLVMState() {}

    static LLVMState get(CompilationContext ctxt) {
        return ctxt.computeAttachmentIfAbsent(KEY, LLVMState::new);
    }

    void addModulePath(LoadedTypeDefinition typeDefinition, Path path) {
        pathsByType.putIfAbsent(typeDefinition, path);
    }

    Map<LoadedTypeDefinition, Path> getModulePaths() {
        return Map.copyOf(pathsByType);
    }

    void registerStatePoints(ProgramModule programModule, List<List<InvocationNode>> fnToIdToNodeMap) {
        var existing = statePointInvocations.putIfAbsent(programModule, fnToIdToNodeMap);
        if (existing != null) {
            throw new IllegalStateException("State point invocations registered twice for " + programModule.getTypeDefinition());
        }
    }

    InvocationNode findInvocationNode(ProgramModule programModule, int fnId, int spId) {
        return statePointInvocations.getOrDefault(programModule, List.of()).get(fnId).get(spId);
    }
}
