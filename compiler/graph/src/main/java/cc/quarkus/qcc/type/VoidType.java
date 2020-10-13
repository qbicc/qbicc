package cc.quarkus.qcc.type;

/**
 *
 */
public final class VoidType extends ValueType {
    VoidType(final TypeSystem typeSystem, final boolean const_) {
        super(typeSystem, VoidType.class.hashCode(), const_);
    }

    public boolean isComplete() {
        return false;
    }

    public long getSize() {
        return 1;
    }

    ValueType constructConst() {
        return new VoidType(typeSystem, true);
    }

    public VoidType asConst() {
        return (VoidType) super.asConst();
    }

    public int getAlign() {
        return 1;
    }

    public boolean equals(final ValueType other) {
        return other instanceof VoidType && super.equals(other);
    }

    public StringBuilder toString(final StringBuilder b) {
        // don't print "incomplete" string
        if (isConst()) {
            b.append("const ");
        }
        return b.append("void");
    }
}
