package org.qbicc.graph;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

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
    private static final ConcurrentHashMap<String, Slot[]> cache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Slot> singletonCache = new ConcurrentHashMap<>();

    private final String name;
    private final int index;
    private final int hashCode;

    Slot(String name) {
        this.name = name;
        index = -1;
        hashCode = name.hashCode();
    }

    Slot(String name, int index) {
        Assert.checkMinimumParameter("index", 0, index);
        Assert.checkMaximumParameter("index", 65535, index);
        this.name = name;
        this.index = index;
        hashCode = Objects.hash(Integer.valueOf(index), name);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Slot other && equals(other);
    }

    public boolean equals(Slot other) {
        return this == other || other != null && index == other.index && name.equals(other.name);
    }

    @Override
    public int compareTo(Slot other) {
        int res = name.compareTo(other.name);
        if (res == 0) res = Integer.compare(index, other.index);
        return res;
    }

    public StringBuilder toString(StringBuilder b) {
        return b.append(name).append(index);
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
        return name;
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

    static Slot get(String name) {
        Slot slot = singletonCache.get(name);
        if (slot == null) {
            slot = new Slot(name);
            Slot appearing = singletonCache.putIfAbsent(name, slot);
            if (appearing != null) {
                slot = appearing;
            }
        }
        return slot;
    }

    static Slot get(String name, int index) {
        Slot[] oldVal, newVal;
        int oldLen;
        for (;;) {
            oldVal = cache.get(name);
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
                newVal[i] = new Slot(name, index);
            }
            if (cache.replace(name, oldVal, newVal)) {
                return newVal[index];
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
        return get("p");
    }

    /**
     * Get the slot for the given index in the {@code "stack"} namespace.
     *
     * @param n the index
     * @return the slot identifier (not {@code null})
     */
    public static Slot stack(int n) {
        return get("stack", n);
    }

    /**
     * Get the slot for the given index in the {@code "tmp"} namespace.
     *
     * @param n the index
     * @return the slot identifier (not {@code null})
     */
    public static Slot temp(int n) {
        return get("tmp", n);
    }

    /**
     * Get the slot for the given index in the {@code "var"} namespace.
     *
     * @param n the index
     * @return the slot identifier (not {@code null})
     */
    public static Slot variable(int n) {
        return get("var", n);
    }

    // singleton

    /**
     * Get the slot for the call result.
     *
     * @return the slot identifier (not {@code null})
     */
    public static Slot result() {
        return get("result");
    }

    /**
     * Get the slot for the {@code jsr} return address.
     *
     * @return the slot identifier (not {@code null})
     */
    public static Slot retAddr() {
        return get("ra");
    }

    /**
     * Get the slot for the current receiver.
     *
     * @return the slot identifier (not {@code null})
     */
    public static Slot this_() {
        return get("this");
    }

    /**
     * Get the slot for the current thread.
     *
     * @return the slot identifier (not {@code null})
     */
    public static Slot thread() {
        return get("thr");
    }

    /**
     * Get the slot for the thrown exception.
     *
     * @return the slot identifier (not {@code null})
     */
    public static Slot thrown() {
        return get("thrown");
    }
}
