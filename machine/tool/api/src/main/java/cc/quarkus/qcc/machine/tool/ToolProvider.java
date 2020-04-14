package cc.quarkus.qcc.machine.tool;

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
    <T extends Tool> Iterable<T> findTools(Class<T> type, final Platform platform);

    static <T extends Tool> Iterable<T> findAllTools(Class<T> type, final Platform platform, Predicate<? super T> filter, ClassLoader classLoader) {
        final List<T> list = new ArrayList<>();
        final ServiceLoader<ToolProvider> loader = ServiceLoader.load(ToolProvider.class, classLoader);
        final Iterator<ToolProvider> iterator = loader.iterator();
        for (;;)
            try {
                if (!iterator.hasNext()) {
                    break;
                }
                for (T t : iterator.next().findTools(type, platform)) {
                    if (filter.test(t)) {
                        list.add(t);
                    }
                }
            } catch (ServiceConfigurationError ignored) {
            }
        return list;
    }
}
