package org.qbicc.plugin.methodinfo.valueinfo;

import io.smallrye.common.constraint.Assert;

/**
 * A value info corresponding to a register-relative value.
 * The reference value is found by adding an offset value to a base register.
 */
public final class RegisterRelativeValueInfo extends ValueInfo {
    private final RegisterValueInfo base;
    private final long offset;

    /**
     * Construct a new instance.
     *
     * @param base the register base (must not be {@code null})
     * @param offset the offset added to the register to locate the value
     */
    public RegisterRelativeValueInfo(RegisterValueInfo base, long offset) {
        super(base.hashCode() * 19 + Long.hashCode(offset));
        this.base = Assert.checkNotNullParam("base", base);
        this.offset = offset;
    }

    public RegisterValueInfo getBase() {
        return base;
    }

    public long getOffset() {
        return offset;
    }

    @Override
    public boolean equals(ValueInfo other) {
        return other instanceof RegisterRelativeValueInfo vi && equals(vi);
    }

    public boolean equals(RegisterRelativeValueInfo other) {
        return super.equals(other) && offset == other.offset && base.equals(other.base);
    }
}
