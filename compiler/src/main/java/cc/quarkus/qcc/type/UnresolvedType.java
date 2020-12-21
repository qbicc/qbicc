package cc.quarkus.qcc.type;

/**
 * The type of any unresolved reference.
 */
public final class UnresolvedType extends ValueType {
    UnresolvedType(final TypeSystem typeSystem, final boolean const_) {
        super(typeSystem, UnresolvedType.class.hashCode(), const_);
    }

    public boolean isComplete() {
        return false;
    }

    public long getSize() {
        return 0;
    }

    ValueType constructConst() {
        return new UnresolvedType(typeSystem, true);
    }

    public int getAlign() {
        return 0;
    }

    public StringBuilder toFriendlyString(final StringBuilder b) {
        return b.append("unresolved");
    }
}
