package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.constraint.Constraint;

final class ArrayElementReadValueImpl extends ArrayElementOperationImpl implements ArrayElementReadValue {
    public Type getType() {
        return ((ArrayType)getInstance().getType()).getElementType();
    }

    public Constraint getConstraint() {
        return null;
    }

    public void setConstraint(final Constraint constraint) {

    }

    public String getLabelForGraph() {
        return "array read";
    }
}
