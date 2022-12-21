package org.qbicc.plugin.source;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.qbicc.context.ClassContext;
import org.qbicc.driver.ClassPathElement;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.descriptor.ClassTypeDescriptor;

/**
 * A listener which copies source files all reachable program elements for use by the debugger.
 */
public final class SourceEmittingElementHandler implements Consumer<ExecutableElement> {
    private final Path outputPath;
    private final Map<ClassContext, List<ClassPathElement>> sourcePaths;
    private final Map<ClassContext, Map<String, Set<String>>> visited = new ConcurrentHashMap<>();

    /**
     * Construct a new instance.
     *
     * @param outputPath the base directory for source output (must not be {@code null})
     * @param sourcePaths the mapping of class path elements (must not be {@code null})
     */
    public SourceEmittingElementHandler(Path outputPath, Map<ClassContext, List<ClassPathElement>> sourcePaths) {
        this.outputPath = outputPath;
        this.sourcePaths = sourcePaths;
    }

    @Override
    public void accept(ExecutableElement executableElement) {
        LoadedTypeDefinition loaded = executableElement.getEnclosingType().load();
        ClassContext classContext = loaded.getContext();
        Map<String, Set<String>> visitedByPackage = visited.computeIfAbsent(classContext, SourceEmittingElementHandler::newConcurrentMap);
        String packageName = loaded.getDescriptor() instanceof ClassTypeDescriptor ctd? ctd.getPackageName() : "";
        Set<String> visitedBySourceFileName = visitedByPackage.computeIfAbsent(packageName, SourceEmittingElementHandler::newConcurrentSet);
        String sourceFileName = executableElement.getSourceFileName();
        if (sourceFileName != null && visitedBySourceFileName.add(sourceFileName)) {
            List<ClassPathElement> elements = sourcePaths.get(classContext);
            if (elements != null) {
                // try to find the source file
                String fullName = packageName.isEmpty() ? sourceFileName : packageName + '/' + sourceFileName;
                for (ClassPathElement element : elements) {
                    try {
                        ClassPathElement.Resource resource = element.getResource(fullName);
                        if (resource != null) {
                            // we found it!
                            Path writePath = outputPath.resolve(packageName);
                            Files.createDirectories(writePath);
                            try (InputStream stream = resource.openStream()) {
                                Files.copy(stream, writePath.resolve(sourceFileName));
                            }
                        }
                    } catch (IOException e) {
                        // ignore this class path element
                    }
                }
            }
        }
    }

    private static <K, V> Map<K, V> newConcurrentMap(final Object ignored) {
        return new ConcurrentHashMap<>();
    }

    private static <E> Set<E> newConcurrentSet(final Object ignored) {
        return ConcurrentHashMap.newKeySet();
    }

}
