package cc.quarkus.qcc.machine.arch;

import static java.lang.reflect.Modifier.*;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
final class Indexer {
    private Indexer() {}

    static <T extends PlatformComponent> Map<String, T> index(Class<T> clazz) {
        final Field[] fields = clazz.getDeclaredFields();
        final Map<String, T> map = new HashMap<>();
        for (Field field : fields) {
            final int mods = field.getModifiers();
            if (isStatic(mods) && isFinal(mods) && isPublic(mods) && clazz.isAssignableFrom(field.getType())) {
                T item;
                try {
                    item = clazz.cast(field.get(null));
                } catch (IllegalAccessException e) {
                    throw new IllegalAccessError(e.getMessage());
                }
                map.put(item.getName(), item);
                for (String alias : item.getAliases()) {
                    map.put(alias, item);
                }
            }
        }
        return Collections.unmodifiableMap(map);
    }
}
