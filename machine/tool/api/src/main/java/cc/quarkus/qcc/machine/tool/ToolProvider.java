package cc.quarkus.qcc.machine.tool;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.function.Predicate;

import cc.quarkus.qcc.machine.arch.Platform;

/**
 *
 */
public interface ToolProvider {
    /**
     * Try to find a tool provider which responds to the given executable path.
     *
     * @param type the tool class (must not be {@code null})
     * @param platform the platform that the tool must support (must not be {@code null})
     * @param path the path to try (must not be {@code null})
     * @param <T> the tool type
     * @return a (possibly empty) iterable containing all of the matching candidate tools
     */
    <T extends Tool> Iterable<T> findTools(Class<T> type, Platform platform, Path path);

    static <T extends Tool> Iterable<T> findAllTools(Class<T> type, final Platform platform, Predicate<? super T> filter, ClassLoader classLoader, List<Path> paths) {
        final List<T> list = new ArrayList<>();
        final ServiceLoader<ToolProvider> loader = ServiceLoader.load(ToolProvider.class, classLoader);
        final Iterator<ToolProvider> iterator = loader.iterator();
        for (;;)
            try {
                if (!iterator.hasNext()) {
                    break;
                }
                ToolProvider item = iterator.next();
                for (Path path : paths) {
                    for (T t : item.findTools(type, platform, path)) {
                        if (filter.test(t)) {
                            list.add(t);
                        }
                    }
                }
            } catch (ServiceConfigurationError ignored) {
            }
        return list;
    }
}
