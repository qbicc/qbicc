package cc.quarkus.qcc.graph2;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class Util {
    private Util() {}

    static <K, V> Map<K, V> mapWithEntry(Map<K, V> orig, K newKey, V newVal) {
        Set<Map.Entry<K, V>> entrySet = orig.entrySet();
        final Iterator<Map.Entry<K, V>> iterator = entrySet.iterator();
        if (! iterator.hasNext()) {
            return Map.of(newKey, newVal);
        }
        Map.Entry<K, V> e1 = iterator.next();
        if (e1.getKey().equals(newKey)) {
            return Map.of(newKey, newVal);
        }
        if (! iterator.hasNext()) {
            return Map.of(e1.getKey(), e1.getValue(), newKey, newVal);
        }
        Map.Entry<K, V> e2 = iterator.next();
        if (e2.getKey().equals(newKey)) {
            return Map.of(e1.getKey(), e1.getValue(), newKey, newVal);
        }
        if (! iterator.hasNext()) {
            return Map.of(e1.getKey(), e1.getValue(), e2.getKey(), e2.getValue(), newKey, newVal);
        }
        Map.Entry<K, V> e3 = iterator.next();
        if (e3.getKey().equals(newKey)) {
            return Map.of(e1.getKey(), e1.getValue(), e2.getKey(), e2.getValue(), newKey, newVal);
        }
        if (! iterator.hasNext()) {
            return Map.of(e1.getKey(), e1.getValue(), e2.getKey(), e2.getValue(), e3.getKey(), e3.getValue(), newKey, newVal);
        }
        Map.Entry<K, V> e4 = iterator.next();
        if (e4.getKey().equals(newKey)) {
            return Map.of(e1.getKey(), e1.getValue(), e2.getKey(), e2.getValue(), e3.getKey(), e3.getValue(), newKey, newVal);
        }
        if (! iterator.hasNext()) {
            return Map.of(e1.getKey(), e1.getValue(), e2.getKey(), e2.getValue(), e3.getKey(), e3.getValue(), e4.getKey(), e4.getValue(), newKey, newVal);
        }
        Map.Entry<K, V> e5 = iterator.next();
        if (e5.getKey().equals(newKey)) {
            return Map.of(e1.getKey(), e1.getValue(), e2.getKey(), e2.getValue(), e3.getKey(), e3.getValue(), e4.getKey(), e4.getValue(), newKey, newVal);
        }
        if (! iterator.hasNext()) {
            return Map.of(e1.getKey(), e1.getValue(), e2.getKey(), e2.getValue(), e3.getKey(), e3.getValue(), e4.getKey(), e4.getValue(), e5.getKey(), e5.getValue(), newKey, newVal);
        }
        // too many
        if (orig.containsKey(newKey)) {
            // slow path
            HashMap<K, V> m = new HashMap<>(orig);
            m.put(newKey, newVal);
            return Map.copyOf(m);
        }
        int size = entrySet.size();
        @SuppressWarnings("unchecked") //
        Map.Entry<K, V>[] entries = entrySet.toArray(new Map.Entry[size + 1]);
        entries[size] = Map.entry(newKey, newVal);
        return Map.ofEntries(entries);
    }

    static <E> Set<E> setWithValue(Class<E> type, Set<E> orig, E newVal) {
        final Iterator<E> iterator = orig.iterator();
        if (! iterator.hasNext()) {
            return Set.of(newVal);
        }
        E e1 = iterator.next();
        if (e1.equals(newVal)) {
            return orig;
        }
        if (! iterator.hasNext()) {
            return Set.of(e1, newVal);
        }
        E e2 = iterator.next();
        if (e2.equals(newVal)) {
            return orig;
        }
        if (! iterator.hasNext()) {
            return Set.of(e1, e2, newVal);
        }
        E e3 = iterator.next();
        if (e3.equals(newVal)) {
            return orig;
        }
        if (! iterator.hasNext()) {
            return Set.of(e1, e2, e3, newVal);
        }
        E e4 = iterator.next();
        if (e4.equals(newVal)) {
            return orig;
        }
        if (! iterator.hasNext()) {
            return Set.of(e1, e2, e3, e4, newVal);
        }
        E e5 = iterator.next();
        if (e5.equals(newVal)) {
            return orig;
        }
        if (! iterator.hasNext()) {
            return Set.of(e1, e2, e3, e4, e5, newVal);
        }
        // too many
        if (orig.contains(newVal)) {
            return orig;
        }
        int size = orig.size();
        @SuppressWarnings("unchecked") //
        E[] elements = orig.toArray((E[]) Array.newInstance(type, size + 1));
        elements[size] = newVal;
        return Set.of(elements);
    }

    static <E> List<E> ListWithValue(Class<E> type, List<E> orig, E newVal) {
        final Iterator<E> iterator = orig.iterator();
        if (! iterator.hasNext()) {
            return List.of(newVal);
        }
        E e1 = iterator.next();
        if (! iterator.hasNext()) {
            return List.of(e1, newVal);
        }
        E e2 = iterator.next();
        if (! iterator.hasNext()) {
            return List.of(e1, e2, newVal);
        }
        E e3 = iterator.next();
        if (! iterator.hasNext()) {
            return List.of(e1, e2, e3, newVal);
        }
        E e4 = iterator.next();
        if (! iterator.hasNext()) {
            return List.of(e1, e2, e3, e4, newVal);
        }
        E e5 = iterator.next();
        if (! iterator.hasNext()) {
            return List.of(e1, e2, e3, e4, e5, newVal);
        }
        // too many
        int size = orig.size();
        @SuppressWarnings("unchecked") //
        E[] elements = orig.toArray((E[]) Array.newInstance(type, size + 1));
        elements[size] = newVal;
        return List.of(elements);
    }
}
