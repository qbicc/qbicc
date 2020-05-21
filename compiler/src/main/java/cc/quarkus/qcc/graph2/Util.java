package cc.quarkus.qcc.graph2;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

final class Util {
    private Util() {}

    static <K, V> Map<K, V> copyMap(Map<K, V> orig, K newKey, V newVal) {
        Set<Map.Entry<K, V>> entrySet = orig.entrySet();
        final Iterator<Map.Entry<K, V>> iterator = entrySet.iterator();
        if (! iterator.hasNext()) {
            return Map.of(newKey, newVal);
        }
        Map.Entry<K, V> e1 = iterator.next();
        if (! iterator.hasNext()) {
            return Map.of(e1.getKey(), e1.getValue(), newKey, newVal);
        }
        Map.Entry<K, V> e2 = iterator.next();
        if (! iterator.hasNext()) {
            return Map.of(e1.getKey(), e1.getValue(), e2.getKey(), e2.getValue(), newKey, newVal);
        }
        Map.Entry<K, V> e3 = iterator.next();
        if (! iterator.hasNext()) {
            return Map.of(e1.getKey(), e1.getValue(), e2.getKey(), e2.getValue(), e3.getKey(), e3.getValue(), newKey, newVal);
        }
        Map.Entry<K, V> e4 = iterator.next();
        if (! iterator.hasNext()) {
            return Map.of(e1.getKey(), e1.getValue(), e2.getKey(), e2.getValue(), e3.getKey(), e3.getValue(), e4.getKey(), e4.getValue(), newKey, newVal);
        }
        Map.Entry<K, V> e5 = iterator.next();
        if (! iterator.hasNext()) {
            return Map.of(e1.getKey(), e1.getValue(), e2.getKey(), e2.getValue(), e3.getKey(), e3.getValue(), e4.getKey(), e4.getValue(), e5.getKey(), e5.getValue(), newKey, newVal);
        }
        // too many
        int size = entrySet.size();
        @SuppressWarnings("unchecked") //
        Map.Entry<K, V>[] entries = entrySet.toArray(new Map.Entry[size + 1]);
        entries[size] = Map.entry(newKey, newVal);
        return Map.ofEntries(entries);
    }

    static void writeGraph(Node rootNode) {

    }
}
