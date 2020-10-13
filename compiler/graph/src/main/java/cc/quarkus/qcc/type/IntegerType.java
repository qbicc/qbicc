package cc.quarkus.qcc.type;

/**
 * An integer type.
 */
public abstract class IntegerType extends NumericType {
    final int size;
    final int align;
    final int minBits;

    IntegerType(final TypeSystem typeSystem, final int hashCode, final boolean const_, final int size, final int align, final int minBits) {
        super(typeSystem, (hashCode * 19 + size) * 19 + minBits, const_);
        this.size = size;
        this.align = align;
        this.minBits = minBits;
    }

    public IntegerType asConst() {
        return (IntegerType) super.asConst();
    }

    public abstract IntegerType getConstraintType();

    public abstract SignedIntegerType asSigned();

    public abstract UnsignedIntegerType asUnsigned();

    public int getAlign() {
        return align;
    }

    public long getSize() {
        return size;
    }

    public int getMinBits() {
        return minBits;
    }
}
