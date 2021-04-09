package org.qbicc.plugin.metrics;

public final class CountMetric extends Metric<CountMetric> {
    CountMetric(String name, CountMetric parent) {
        super(name, parent);
    }

    @Override
    CountMetric constructChild(String name) {
        return new CountMetric(name, this);
    }

    public void add(long amount) {
        addRawValue(amount);
    }

    @Override
    String getDescription() {
        return "Count";
    }

    @Override
    public StringBuilder getFormattedValue(StringBuilder target) {
        return target.append(Long.toUnsignedString(getRawValue()));
    }
}
