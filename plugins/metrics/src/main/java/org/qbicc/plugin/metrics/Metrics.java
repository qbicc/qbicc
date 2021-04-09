package org.qbicc.plugin.metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
public final class Metrics {
    private static final AttachmentKey<Metrics> KEY = new AttachmentKey<>();
    private final Map<String, MemorySizeMetric> memorySizes = new ConcurrentHashMap<>();
    private final Map<String, CountMetric> counts = new ConcurrentHashMap<>();
    private final Map<String, Timer> timers = new ConcurrentHashMap<>();

    private Metrics() {}

    public static Metrics get(CompilationContext ctxt) {
        Metrics metrics = ctxt.getAttachment(KEY);
        if (metrics == null) {
            metrics = new Metrics();
            Metrics appearing = ctxt.putAttachmentIfAbsent(KEY, metrics);
            if (appearing != null) {
                metrics = appearing;
            }
        }
        return metrics;
    }

    public Timer getOrCreateRootTimer(String name) {
        Assert.checkNotNullParam("name", name);
        Timer metric = timers.get(name);
        if (metric == null) {
            // avoid creating a capturing lambda by using putIfAbsent instead of computeIfAbsent
            metric = new Timer(name, null);
            Timer appearing = timers.putIfAbsent(name, metric);
            if (appearing != null) {
                metric = appearing;
            }
        }
        return metric;
    }

    public MemorySizeMetric getOrCreateRootMemorySizeMetric(String name) {
        Assert.checkNotNullParam("name", name);
        MemorySizeMetric metric = memorySizes.get(name);
        if (metric == null) {
            // avoid creating a capturing lambda by using putIfAbsent instead of computeIfAbsent
            metric = new MemorySizeMetric(name, null);
            MemorySizeMetric appearing = memorySizes.putIfAbsent(name, metric);
            if (appearing != null) {
                metric = appearing;
            }
        }
        return metric;
    }

    public CountMetric getOrCreateRootCountMetric(String name) {
        Assert.checkNotNullParam("name", name);
        CountMetric metric = counts.get(name);
        if (metric == null) {
            // avoid creating a capturing lambda by using putIfAbsent instead of computeIfAbsent
            metric = new CountMetric(name, null);
            CountMetric appearing = counts.putIfAbsent(name, metric);
            if (appearing != null) {
                metric = appearing;
            }
        }
        return metric;
    }

    public StringBuilder formatAll(StringBuilder target) {
        formatAllOf(target, timers);
        formatAllOf(target, memorySizes);
        formatAllOf(target, counts);
        return target;
    }

    private void formatAllOf(StringBuilder target, Map<String, ? extends Metric<?>> map) {
        for (Metric<?> metric : map.values()) {
            formatMetric(target, metric, 0);
        }
    }

    private void formatMetric(StringBuilder target, Metric<?> metric, int depth) {
        tab(target, depth);
        target.append(metric.getName()).append(':').append('\t');
        metric.getFormattedValue(target);
        target.append(System.lineSeparator());
        for (Metric<?> child : metric.getChildren()) {
            formatMetric(target, child, depth + 1);
        }
    }

    @SuppressWarnings("StringRepeatCanBeUsed")
    private void tab(StringBuilder target, int count) {
        // do not use String.repeat() to avoid creating transient objects
        for (int i = 0; i < count; i ++) {
            target.append('\t');
        }
    }
}
