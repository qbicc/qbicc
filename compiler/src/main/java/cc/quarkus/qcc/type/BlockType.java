package cc.quarkus.qcc.type;

import io.smallrye.common.constraint.Assert;

/**
 * A special type representing a return address for bytecode {@code jsr}/{@code ret} processing.
 */
public final class BlockType extends ValueType {

    BlockType(TypeSystem typeSystem) {
        super(typeSystem, BlockType.class.hashCode(), true);
    }

    public long getSize() {
        return 0;
    }

    public int getAlign() {
        return 0;
    }

    public boolean isComplete() {
        return false;
    }

    public ValueType asConst() {
        return this;
    }

    ValueType constructConst() {
        throw Assert.unreachableCode();
    }

    public boolean equals(final ValueType other) {
        return other instanceof BlockType && super.equals(other);
    }

    public StringBuilder toString(final StringBuilder b) {
        return b.append("ret_addr");
    }
}
