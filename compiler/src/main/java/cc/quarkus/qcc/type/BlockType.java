package cc.quarkus.qcc.type;

/**
 * A special type representing a return address for bytecode {@code jsr}/{@code ret} processing.
 */
public final class BlockType extends ValueType {

    BlockType(TypeSystem typeSystem) {
        super(typeSystem, BlockType.class.hashCode());
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

    public boolean equals(final ValueType other) {
        return other instanceof BlockType && super.equals(other);
    }

    public StringBuilder toString(final StringBuilder b) {
        return b.append("ret_addr");
    }

    public StringBuilder toFriendlyString(final StringBuilder b) {
        return b.append("ret_addr");
    }
}
