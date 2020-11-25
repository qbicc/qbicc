package cc.quarkus.qcc.type;

/**
 * The type of values that represent the run time type of an object.  Values of this type have a total order.  Values
 * of this type which correspond to classes or arrays of classes form a tree, and values of this type which correspond
 * to interfaces or arrays of interfaces form a DAG.
 */
public final class TypeIdType extends ScalarType {
    private final int align;
    private final int size;

    TypeIdType(final TypeSystem typeSystem, final int size, final int align, final boolean const_) {
        super(typeSystem, TypeIdType.class.hashCode() * 19 + size, const_);
        this.size = size;
        this.align = align;
    }

    public int getAlign() {
        return align;
    }

    public long getSize() {
        return size;
    }

    ValueType constructConst() {
        return new TypeIdType(typeSystem, size, align, true);
    }

    public boolean equals(final ValueType other) {
        return other instanceof TypeIdType && equals((TypeIdType) other);
    }

    public boolean equals(final TypeIdType other) {
        return this == other || super.equals(other) && size == other.size;
    }

    public StringBuilder toString(final StringBuilder b) {
        return super.toString(b).append("typeId");
    }

    public StringBuilder toFriendlyString(final StringBuilder b) {
        return b.append("typeid");
    }

    public TypeIdType asConst() {
        return (TypeIdType) super.asConst();
    }
}
