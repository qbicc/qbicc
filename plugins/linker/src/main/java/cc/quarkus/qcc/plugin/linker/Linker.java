package org.qbicc.plugin.linker;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;

/**
 * The image linker API.
 */
public final class Linker {
    private static final AttachmentKey<Linker> KEY = new AttachmentKey<>();

    private final List<Path> objectPaths = new ArrayList<>();
    private final Set<String> libraries = ConcurrentHashMap.newKeySet();

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

    public void addLibrary(String library) {
        libraries.add(library);
    }

    public List<String> getLibraries() {
        return List.copyOf(libraries);
    }
}
