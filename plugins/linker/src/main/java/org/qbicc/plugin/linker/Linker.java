package org.qbicc.plugin.linker;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.type.definition.LoadedTypeDefinition;

/**
 * The image linker API.
 */
public final class Linker {
    private static final AttachmentKey<Linker> KEY = new AttachmentKey<>();

    private final Map<LoadedTypeDefinition, Path> objectPathsByType = new ConcurrentHashMap<>();
    private final Set<String> libraries = ConcurrentHashMap.newKeySet();
    private final CompilationContext ctxt;

    private Linker(CompilationContext ctxt) {
        this.ctxt = ctxt;
    }

    public static Linker get(CompilationContext ctxt) {
        return ctxt.computeAttachmentIfAbsent(KEY, () -> new Linker(ctxt));
    }

    public void addObjectFilePath(LoadedTypeDefinition typeDefinition, Path objectFilePath) {
        if (objectFilePath != null) {
            if (objectPathsByType.putIfAbsent(typeDefinition, objectFilePath) != null) {
                ctxt.error("Multiple object files for type %s", typeDefinition);
            }
        }
    }

    public List<Path> getObjectFilePathsInLinkOrder() {
        List<LoadedTypeDefinition> types = new ArrayList<>(objectPathsByType.keySet());
        types.sort(Comparator.comparingInt(LoadedTypeDefinition::getTypeId));
        List<Path> sortedPaths = new ArrayList<>(types.size());
        for (LoadedTypeDefinition type : types) {
            sortedPaths.add(objectPathsByType.get(type));
        }
        return sortedPaths;
    }

    public void addLibrary(String library) {
        libraries.add(library);
    }

    public List<String> getLibraries() {
        return List.copyOf(libraries);
    }
}
