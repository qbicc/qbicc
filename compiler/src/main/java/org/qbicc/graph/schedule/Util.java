package org.qbicc.graph.schedule;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.stream.Stream;

import io.smallrye.common.constraint.Assert;

public final class Util {
    private Util() {}

    /**
     * Get an immutable, cached copy of the given (possibly mutable) set from the given cache.
     * The given set can be modified after this call.
     *
     * @param setCache the set cache (must not be {@code null})
     * @param set the set to copy (must not be {@code null})
     * @return the copied set (not {@code null})
     */
    public static <E> Set<E> getCachedSet(Map<Set<E>, Set<E>> setCache, Set<E> set) {
        if (set.isEmpty()) {
            return Set.of();
        }
        Set<E> cached = setCache.get(set);
        if (cached == null) {
            cached = Util.copyOfTrusted(set);
            final Set<E> appearing = setCache.putIfAbsent(cached, cached);
            if (appearing != null) {
                cached = appearing;
            }
        }
        return cached;
    }

    /**
     * Copy the given set to an immutable set, without constructing an intermediate {@code HashSet}.
     *
     * @param orig the original set (must not be {@code null})
     * @return the immutable copy (not {@code null})
     * @param <E> the element type
     */
    public static <E> Set<E> copyOfTrusted(Set<E> orig) {
        if (orig.isEmpty()) {
            return Set.of();
        } else {
            return Set.of((E[])orig.toArray(Object[]::new));
        }
    }

    /**
     * Create an instance of a tiny (immutable) set implementation that adds an item to an existing (immutable) set.
     * If the original set already contains the item, it is returned as-is.
     * Use for short-lived keys into hash tables that are keyed by set.
     *
     * @param orig the original set (must not be {@code null})
     * @param item the item to add (must not be {@code null})
     * @return the union set (not {@code null} or empty)
     * @param <E> the item type
     */
    public static <E> Set<E> setWith(Set<E> orig, E item) {
        Assert.checkNotNullParam("orig", orig);
        Assert.checkNotNullParam("item", item);
        if (orig.isEmpty()) {
            return Set.of(item);
        } else if (orig.contains(item)) {
            return orig;
        } else {
            return new WithSet<>(orig, item);
        }
    }

    /**
     * A tiny set object which adds one item to another set.
     */
    private static final class WithSet<E> extends AbstractSet<E> {
        private final E item;
        private final Set<E> orig;

        private WithSet(Set<E> orig, E item) {
            this.item = item;
            this.orig = orig;
        }



        @Override
        public Iterator<E> iterator() {
            final Iterator<E> delegate = orig.iterator();
            return new Iterator<E>() {
                private E next = item;

                @Override
                public boolean hasNext() {
                    if (next != null) {
                        return true;
                    } else if (delegate.hasNext()) {
                        next = delegate.next();
                        return true;
                    } else {
                        return false;
                    }
                }

                @Override
                public E next() {
                    if (! hasNext()) throw new NoSuchElementException();
                    try {
                        return next;
                    } finally {
                        next = null;
                    }
                }
            };
        }

        @Override
        public int size() {
            return orig.size() + 1;
        }

        @Override
        public int hashCode() {
            return orig.hashCode() + item.hashCode();
        }

        @SuppressWarnings("SuspiciousMethodCalls")
        @Override
        public boolean equals(Object other) {
            return other == this || other instanceof Set<?> set && size() == set.size() && hashCode() == set.hashCode() && set.contains(item) && set.containsAll(orig);
        }

        @Override
        public boolean contains(Object o) {
            return item.equals(o) || orig.contains(o);
        }

        @Override
        public Stream<E> stream() {
            return Stream.concat(Stream.of(item), orig.stream());
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Object[] toArray() {
            return toArray(Object[]::new);
        }

        @Override
        public <T> T[] toArray(T[] a) {
            if (a.length >= size()) {
                return toArray(ignored -> a);
            } else {
                return toArray(size -> Arrays.copyOf(a, size));
            }
        }

        @Override
        public <T> T[] toArray(IntFunction<T[]> generator) {
            final int size = size();
            T[] array = orig.toArray(generator.apply(size));
            array[size - 1] = (T) item;
            return array;
        }
    }
}

