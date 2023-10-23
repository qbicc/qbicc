package org.qbicc.machine.arch;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 *
 */
final class Indexer {
    private Indexer() {}

    static <T extends Enum<T> & PlatformComponent> Map<String, T> index(Class<T> clazz) {
        T[] enumConstants = clazz.getEnumConstants();
        final Map<String, T> map = new HashMap<>();
        for (T item : enumConstants) {
            map.put(item.name().toLowerCase(Locale.ROOT), item);
            for (String alias : item.aliases()) {
                map.put(alias.toLowerCase(Locale.ROOT), item);
            }
        }
        return Map.copyOf(map);
    }
}
