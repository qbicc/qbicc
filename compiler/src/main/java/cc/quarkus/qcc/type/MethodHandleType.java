package cc.quarkus.qcc.type;

import io.smallrye.common.constraint.Assert;

/**
 * The type representing method handle literals, which are always {@code const}, have no size and are incomplete.
 */
public final class MethodHandleType extends ValueType {
  MethodHandleType(final TypeSystem typeSystem) {
        super(typeSystem, MethodHandleType.class.hashCode(), true);
    }

    public boolean isComplete() {
        return false;
    }

    public long getSize() {
        return 0;
    }

    ValueType constructConst() {
        throw Assert.unreachableCode();
    }

    public MethodHandleType asConst() {
        return this;
    }

    public int getAlign() {
        return 0;
    }

    public boolean equals(final ValueType other) {
        return other instanceof MethodHandleType && super.equals(other);
    }

    public StringBuilder toString(final StringBuilder b) {
        return b.append("methodhandle");
    }

    public StringBuilder toFriendlyString(final StringBuilder b) {
        return b.append("mh");
    }
}
