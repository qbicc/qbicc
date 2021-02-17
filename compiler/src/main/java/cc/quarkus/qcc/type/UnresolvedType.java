package cc.quarkus.qcc.type;

/**
 * The type of any unresolved reference.
 */
public final class UnresolvedType extends ValueType {
    UnresolvedType(final TypeSystem typeSystem) {
        super(typeSystem, UnresolvedType.class.hashCode());
    }

    public boolean isComplete() {
        return false;
    }

    public long getSize() {
        return 0;
    }

    public int getAlign() {
        return 0;
    }

    public StringBuilder toFriendlyString(final StringBuilder b) {
        return b.append("unresolved");
    }
}
