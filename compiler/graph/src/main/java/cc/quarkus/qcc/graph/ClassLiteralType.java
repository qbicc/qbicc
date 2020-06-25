package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.constraint.Constraint;

/**
 * A type representing a class literal, before it is promoted to an object.
 */
public interface ClassLiteralType extends Type {
    default boolean isAssignableFrom(Type otherType) {
        return otherType instanceof ClassLiteralType;
    }

    default int getParameterCount() {
        return 0;
    }

    default String getParameterName(int index) throws IndexOutOfBoundsException {
        throw new IndexOutOfBoundsException(index);
    }

    default Constraint getParameterConstraint(int index) throws IndexOutOfBoundsException {
        throw new IndexOutOfBoundsException(index);
    }
}
