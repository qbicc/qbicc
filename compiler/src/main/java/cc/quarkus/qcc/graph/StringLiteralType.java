package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.constraint.Constraint;

/**
 *
 */
public interface StringLiteralType extends Type {
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
