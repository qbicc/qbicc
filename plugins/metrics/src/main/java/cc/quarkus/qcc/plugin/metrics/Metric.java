package cc.quarkus.qcc.plugin.metrics;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import io.smallrye.common.constraint.Assert;

/**
 *
 */
public abstract class Metric<M extends Metric<M>> {
    private final String name;
    private final M parent;
    private final AtomicLong cnt = new AtomicLong();
    private final Map<String, M> children = new ConcurrentHashMap<>();

    Metric(String name, M parent) {
        this.name = name;
        this.parent = parent;
    }

    abstract M constructChild(String name);

    /**
     * Get or add a child with the given name, adding it if it none is present.
     *
     * @param name the name of the child (must not be {@code null})
     * @return the child (not {@code null})
     */
    public final M getOrAddChild(String name) {
        Assert.checkNotNullParam("name", name);
        M child = children.get(name);
        if (child == null) {
            child = constructChild(name);
            M appearing = children.putIfAbsent(name, child);
            if (appearing != null) {
                child = appearing;
            }
        }
        return child;
    }

    /**
     * Get or add a child from the given path.  This is equivalent to calling {@link #getOrAddChild(String)}
     * repeatedly for each given name.
     *
     * @param names a path of names leading to the child (must not be {@code null} nor contain {@code null} elements)
     * @return the child corresponding to the given name path (not {@code null})
     */
    public M getOrAddChild(String... names) {
        M child = (M) this;
        for (String name : names) {
            child = child.getOrAddChild(name);
        }
        return child;
    }

    /**
     * Get the name of this metric.
     *
     * @return the name of this metric (not {@code null})
     */
    public final String getName() {
        return name;
    }

    public final boolean isChildOf(Metric<?> other) {
        return other != null && other.isParentOf(this);
    }

    public final boolean isParentOf(Metric<?> other) {
        return other != null && (other == this || isParentOf(other.parent));
    }

    /**
     * Get the parent of this metric, which will be of the same type.
     *
     * @return the parent of this metric
     */
    public final M getParent() {
        return parent;
    }

    /**
     * Get the raw value of this metric as an unsigned value.
     *
     * @return the raw value
     */
    public long getRawValue() {
        return cnt.get();
    }

    final void addRawValue(long amount) {
        cnt.getAndAdd(amount);
        M parent = getParent();
        if (parent != null) {
            parent.addRawValue(amount);
        }
    }

    /**
     * Get all the existent children of this metric.  The children are of the same type as this instance.
     *
     * @return the collection of children (not {@code null})
     */
    public final Collection<M> getChildren() {
        return children.values();
    }

    /**
     * Get the current value of this metric, formatted readably to the given string builder.
     *
     * @param target the string builder (must not be {@code null})
     * @return the same string builder
     */
    public abstract StringBuilder getFormattedValue(StringBuilder target);

    /**
     * Get the current value of this metric, formatted readably as a string.
     *
     * @return the formatted string (not {@code null})
     */
    public final String getFormattedValue() {
        return getFormattedValue(new StringBuilder()).toString();
    }

    abstract String getDescription();

    /**
     * Get the full path of this metric, formatted readably to the given string builder.
     *
     * @param target the string builder (must not be {@code null})
     * @return the same string builder
     */
    public final StringBuilder toString(StringBuilder target) {
        M parent = getParent();
        if (parent != null) {
            parent.toString(target);
            target.append(":");
        } else {
            target.append(getDescription()).append(' ');
        }
        return target.append(name);
    }

    @Override
    public final String toString() {
        return toString(new StringBuilder()).toString();
    }
}
