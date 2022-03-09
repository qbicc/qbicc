package org.qbicc.type;

/**
 *
 */
public final class VoidType extends ValueType {
    VoidType(final TypeSystem typeSystem) {
        super(typeSystem, VoidType.class.hashCode());
    }

    public boolean isComplete() {
        return false;
    }

    public long getSize() {
        return 1;
    }

    public int getAlign() {
        return 1;
    }

    public boolean equals(final ValueType other) {
        return other instanceof VoidType && super.equals(other);
    }

    public StringBuilder toString(final StringBuilder b) {
        // don't print "incomplete" string
        return b.append("void");
    }

    public StringBuilder toFriendlyString(final StringBuilder b) {
        return b.append("void");
    }

    public Primitive asPrimitive() {
        return Primitive.VOID;
    }
}
