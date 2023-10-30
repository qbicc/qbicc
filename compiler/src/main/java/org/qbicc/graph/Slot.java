package org.qbicc.graph;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import io.smallrye.common.constraint.Assert;

/**
 * An identifier for a basic block argument, which are <em>named</em> (i.e. not positional) arguments.
 * <p>
 * The identifier consists of a namespace string and an optional index.
 * Identifiers are cached statically.
 * <p>
 * Basic block parameters are identified by the combination of slot and declaring block.
 */
public final class Slot implements Comparable<Slot> {
    private static final ConcurrentHashMap<Kind, Slot[]> cache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, List<Slot>> simpleArgListCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<List<Slot>, List<Slot>> addingThreadCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<List<Slot>, List<Slot>> addingThisCache = new ConcurrentHashMap<>();

    private static final Slot THREAD = new Slot(Kind.thread);
    private static final Slot THIS = new Slot(Kind.this_);
    private static final Slot RESULT = new Slot(Kind.result);
    private static final Slot THROWN = new Slot(Kind.thrown);

    private final Kind kind;
    private final int index;

    Slot(Kind kind) {
        this.kind = kind;
        index = -1;
    }

    Slot(Kind kind, int index) {
        Assert.checkMinimumParameter("index", 0, index);
        Assert.checkMaximumParameter("index", 65535, index);
        this.kind = kind;
        this.index = index;
    }

    @Override
    public int compareTo(Slot other) {
        int res = kind.compareTo(other.kind);
        if (res == 0) res = Integer.compare(index, other.index);
        return res;
    }

    public StringBuilder toString(StringBuilder b) {
        b.append(kind);
        if (index != -1) {
            b.append(index);
        }
        return b;
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    /**
     * Get the slot name.
     *
     * @return the slot name (not {@code null})
     */
    public String getName() {
        return kind.toString();
    }

    /**
     * Get the slot index.
     *
     * @return the slot index
     */
    public int getIndex() {
        int index = this.index;
        return index == -1 ? 0 : index;
    }

    public Kind kind() {
        return kind;
    }

    static Slot get(Kind kind, int index) {
        Slot[] oldVal, newVal;
        int oldLen;
        oldVal = cache.get(kind);
        for (;;) {
            if (oldVal == null) {
                // create
                oldLen = 0;
                newVal = new Slot[index + 1];
            } else {
                oldLen = oldVal.length;
                if (oldLen > index) {
                    return oldVal[index];
                } else {
                    // grow
                    newVal = Arrays.copyOf(oldVal, index + 1);
                }
            }
            for (int i = oldLen; i <= index; i ++) {
                newVal[i] = new Slot(kind, i);
            }
            if (oldVal == null) {
                Slot[] appearing = cache.putIfAbsent(kind, newVal);
                if (appearing != null) {
                    oldVal = appearing;
                } else {
                    return newVal[index];
                }
            } else {
                if (cache.replace(kind, oldVal, newVal)) {
                    return newVal[index];
                } else {
                    oldVal = cache.get(kind);
                }
            }
        }
    }

    // indexed

    /**
     * Get the slot for the given index in the {@code "p"} namespace (function positional parameter).
     *
     * @param n the index
     * @return the slot identifier (not {@code null})
     */
    public static Slot funcParam(int n) {
        return get(Kind.param, n);
    }

    /**
     * Get the slot for the given index in the {@code "stack"} namespace.
     *
     * @param n the index
     * @return the slot identifier (not {@code null})
     */
    public static Slot stack(int n) {
        return get(Kind.stack, n);
    }

    /**
     * Get the slot for the given index in the {@code "tmp"} namespace.
     *
     * @param n the index
     * @return the slot identifier (not {@code null})
     */
    public static Slot temp(int n) {
        return get(Kind.temp, n);
    }

    /**
     * Get the slot for the given index in the {@code "var"} namespace.
     *
     * @param n the index
     * @return the slot identifier (not {@code null})
     */
    public static Slot variable(int n) {
        return get(Kind.variable, n);
    }

    // singleton

    /**
     * Get the slot for the call result.
     *
     * @return the slot identifier (not {@code null})
     */
    public static Slot result() {
        return RESULT;
    }

    /**
     * Get the slot for the current receiver.
     *
     * @return the slot identifier (not {@code null})
     */
    public static Slot this_() {
        return THIS;
    }

    /**
     * Get the slot for the current thread.
     *
     * @return the slot identifier (not {@code null})
     */
    public static Slot thread() {
        return THREAD;
    }

    /**
     * Get the slot for the thrown exception.
     *
     * @return the slot identifier (not {@code null})
     */
    public static Slot thrown() {
        return THROWN;
    }

    /**
     * Get a cached list of slots corresponding to a simple positional argument list.
     *
     * @param cnt the number of arguments
     * @return the list with that many arguments
     */
    public static List<Slot> simpleArgList(int cnt) {
        Assert.checkMinimumParameter("cnt", 0, cnt);
        Assert.checkMaximumParameter("cnt", 65535, cnt);
        if (cnt == 0) {
            return List.of();
        }
        Integer boxed = Integer.valueOf(cnt);
        List<Slot> list = simpleArgListCache.get(boxed);
        if (list == null) {
            list = IntStream.range(0, cnt).mapToObj(Slot::funcParam).toList();
            List<Slot> appearing = simpleArgListCache.putIfAbsent(boxed, list);
            if (appearing != null) {
                list = appearing;
            }
        }
        return list;
    }

    /**
     * Get a cached list of slots that is the same as the given original list, but with the receiver slot prepended.
     *
     * @param original the original list (must not be {@code null})
     * @return the prepended list (not {@code null})
     */
    public static List<Slot> argListWithPrependedThis(List<Slot> original) {
        Assert.checkNotNullParam("original", original);
        List<Slot> list = addingThisCache.get(original);
        if (list == null) {
            list = Stream.concat(Stream.of(Slot.this_()), original.stream()).toList();
            List<Slot> appearing = addingThisCache.putIfAbsent(original, list);
            if (appearing != null) {
                list = appearing;
            }
        }
        return list;
    }

    /**
     * Get a cached list of slots that is the same as the given original list, but with the {@code thread} slot prepended.
     *
     * @param original the original list (must not be {@code null})
     * @return the prepended list (not {@code null})
     */
    public static List<Slot> argListWithPrependedThread(List<Slot> original) {
        Assert.checkNotNullParam("original", original);
        List<Slot> list = addingThreadCache.get(original);
        if (list == null) {
            list = Stream.concat(Stream.of(Slot.thread()), original.stream()).toList();
            List<Slot> appearing = addingThreadCache.putIfAbsent(original, list);
            if (appearing != null) {
                list = appearing;
            }
        }
        return list;
    }

    public enum Kind {
        thread("thr"),
        this_("this"),
        param("p"),
        result,
        thrown,
        stack("stack"),
        variable("var"),
        temp("tmp"),
        ;
        private final String toString;

        Kind() {
            toString = name();
        }

        Kind(String str) {
            toString = str;
        }

        @Override
        public String toString() {
            return toString;
        }
    }
}
