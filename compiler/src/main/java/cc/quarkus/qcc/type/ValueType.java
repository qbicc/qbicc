package cc.quarkus.qcc.type;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * An "object" in memory, which consists of word types and structured types.
 */
public abstract class ValueType extends Type {
    private static final VarHandle constTypeHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "constType", VarHandle.class, ValueType.class, ValueType.class);

    private volatile ValueType constType;

    ValueType(final TypeSystem typeSystem, final int hashCode, final boolean const_) {
        super(typeSystem, hashCode * 19 + Boolean.hashCode(const_));
        if (const_) {
            constType = this;
        }
    }

    /**
     * Get the size of this type, in "bytes" as defined by {@linkplain TypeSystem#getByteBits() the platform byte size}.
     *
     * @return the size
     */
    public abstract long getSize();

    public boolean isComplete() {
        return true;
    }

    abstract ValueType constructConst();

    public ValueType asConst() {
        ValueType constType = this.constType;
        if (constType != null) {
            return constType;
        }
        ValueType newConstType = constructConst();
        while (! constTypeHandle.compareAndSet(this, null, newConstType)) {
            constType = this.constType;
            if (constType != null) {
                return constType;
            }
        }
        return newConstType;
    }

    public boolean isConst() {
        return this == constType;
    }

    public final boolean equals(final Type other) {
        return other instanceof ValueType && equals((ValueType) other);
    }

    public boolean equals(final ValueType other) {
        return this == other || super.equals(other) && isConst() == other.isConst();
    }

    public ValueType join(final ValueType other) {
        boolean const_ = isConst() || other.isConst();
        if (asConst() == other.asConst()) {
            return const_ ? asConst() : this;
        }
        throw new IllegalArgumentException("Types " + this + " and " + other + " do not join");
    }

    public StringBuilder toString(final StringBuilder b) {
        if (! isComplete()) b.append("incomplete ");
        if (isConst()) b.append("const ");
        return b;
    }

    public abstract int getAlign();
}
