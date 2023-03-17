package org.qbicc.plugin.methodinfo.valueinfo;

import io.smallrye.common.constraint.Assert;

/**
 * A value info corresponding to a stack-allocated value.
 */
public final class FrameOffsetValueInfo extends ValueInfo {
    private final RegisterValueInfo base;
    private final int offset;

    /**
     * Construct a new instance.
     *
     * @param base the register base (must not be {@code null})
     * @param offset the offset added to the register to locate the value in reference-sized words
     */
    public FrameOffsetValueInfo(RegisterValueInfo base, int offset) {
        super(base.hashCode() * 19 + Integer.hashCode(offset));
        this.base = Assert.checkNotNullParam("base", base);
        this.offset = offset;
    }

    public RegisterValueInfo getBase() {
        return base;
    }

    /**
     * Get the offset value.
     *
     * @return the offset value, in reference-sized words (may be negative)
     */
    public int getOffset() {
        return offset;
    }

    @Override
    public boolean equals(ValueInfo other) {
        return other instanceof FrameOffsetValueInfo vi && equals(vi);
    }

    public boolean equals(FrameOffsetValueInfo other) {
        return super.equals(other) && offset == other.offset && base.equals(other.base);
    }
}
