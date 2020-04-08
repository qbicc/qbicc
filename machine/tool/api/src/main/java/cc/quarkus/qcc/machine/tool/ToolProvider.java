package cc.quarkus.qcc.machine.tool;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 *
 */
public interface ToolProvider {
    <T extends Tool> Iterable<T> findTools(Class<T> type);

    static <T extends Tool> Iterable<T> findAllTools(Class<T> type, ClassLoader classLoader) {
        final List<T> list = new ArrayList<>();
        final ServiceLoader<ToolProvider> loader = ServiceLoader.load(ToolProvider.class);
        final Iterator<ToolProvider> iterator = loader.iterator();
        for (;;)
            try {
                if (!iterator.hasNext()) {
                    break;
                }
                iterator.next().findTools(type).forEach(list::add);
            } catch (ServiceConfigurationError ignored) {
            }
        return list;
    }
}
