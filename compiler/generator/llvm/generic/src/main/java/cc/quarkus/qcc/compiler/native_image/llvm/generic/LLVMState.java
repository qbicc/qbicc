package cc.quarkus.qcc.compiler.native_image.llvm.generic;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cc.quarkus.qcc.context.AttachmentKey;

/**
 *
 */
final class LLVMState {
    static final AttachmentKey<LLVMState> KEY = new AttachmentKey<>();

    private final List<Path> modulePaths = Collections.synchronizedList(new ArrayList<>());

    LLVMState() {}

    void addModulePath(Path path) {
        modulePaths.add(path);
    }

    List<Path> getModulePaths() {
        synchronized (modulePaths) {
            return List.copyOf(modulePaths);
        }
    }

}
