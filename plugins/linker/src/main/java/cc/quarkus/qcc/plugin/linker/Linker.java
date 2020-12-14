package cc.quarkus.qcc.plugin.linker;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.context.AttachmentKey;
import cc.quarkus.qcc.context.CompilationContext;

/**
 * The image linker API.
 */
public final class Linker {
    private static final AttachmentKey<Linker> KEY = new AttachmentKey<>();

    private final List<Path> objectPaths = new ArrayList<>();

    private Linker() {}

    public static Linker get(CompilationContext ctxt) {
        return ctxt.computeAttachmentIfAbsent(KEY, Linker::new);
    }

    public void addObjectFilePath(Path objectFilePath) {
        if (objectFilePath != null) {
            List<Path> objectPaths = this.objectPaths;
            synchronized (objectPaths) {
                this.objectPaths.add(objectFilePath);
            }
        }
    }

    public List<Path> getObjectFilePaths() {
        synchronized (objectPaths) {
            return List.copyOf(objectPaths);
        }
    }
}
