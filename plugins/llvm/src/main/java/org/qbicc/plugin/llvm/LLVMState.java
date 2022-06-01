package org.qbicc.plugin.llvm;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.qbicc.context.AttachmentKey;
import org.qbicc.type.definition.LoadedTypeDefinition;

/**
 *
 */
final class LLVMState {
    static final AttachmentKey<LLVMState> KEY = new AttachmentKey<>();

    private final Map<LoadedTypeDefinition, Path> pathsByType = new ConcurrentHashMap<>();

    LLVMState() {}

    void addModulePath(LoadedTypeDefinition typeDefinition, Path path) {
        pathsByType.putIfAbsent(typeDefinition, path);
    }

    Map<LoadedTypeDefinition, Path> getModulePaths() {
        return new HashMap<>(pathsByType);
    }
}
