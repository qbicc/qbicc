package org.qbicc.plugin.methodinfo.valueinfo;

import io.smallrye.common.constraint.Assert;
import org.qbicc.graph.literal.Literal;

/**
 * A value info corresponding to a constant value.
 */
public final class ConstantValueInfo extends ValueInfo {
    private final Literal value;

    /**
     * Construct a new instance.
     *
     * @param value the constant value
     */
    public ConstantValueInfo(final Literal value) {
        super(value.hashCode());
        this.value = Assert.checkNotNullParam("value", value);
    }

    public Literal getValue() {
        return value;
    }

    @Override
    public boolean equals(ValueInfo other) {
        return other instanceof ConstantValueInfo cvi && equals(cvi);
    }

    public boolean equals(ConstantValueInfo other) {
        return super.equals(other) && value.equals(other.value);
    }
}
