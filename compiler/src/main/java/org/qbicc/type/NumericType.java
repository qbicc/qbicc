package org.qbicc.type;

/**
 * A word type that represents a numeric value.
 */
public abstract class NumericType extends WordType {
    NumericType(final TypeSystem typeSystem, final int hashCode) {
        super(typeSystem, hashCode);
    }

    public abstract NumericType getConstraintType();
}
