package org.qbicc.plugin.llvm;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.qbicc.context.AttachmentKey;

/**
 *
 */
final class LLVMState {
    static final AttachmentKey<LLVMState> KEY = new AttachmentKey<>();

    private final List<Path> modulePaths = Collections.synchronizedList(new ArrayList<>());
    private Path defaultModulePath;

    LLVMState() {}

    void addModulePath(Path path) {
        modulePaths.add(path);
    }

    void setDefaultModulePath(Path path) { defaultModulePath = path; }

    List<Path> getModulePaths() {
        synchronized (modulePaths) {
            return List.copyOf(modulePaths);
        }
    }

    Path getDefaultModulePath() {
        return defaultModulePath;
    }
}
