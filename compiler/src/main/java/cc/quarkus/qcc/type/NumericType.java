package cc.quarkus.qcc.type;

/**
 * A word type that represents a numeric value.
 */
public abstract class NumericType extends WordType {
    NumericType(final TypeSystem typeSystem, final int hashCode, final boolean const_) {
        super(typeSystem, hashCode, const_);
    }

    public NumericType asConst() {
        return (NumericType) super.asConst();
    }

    public abstract NumericType getConstraintType();
}
