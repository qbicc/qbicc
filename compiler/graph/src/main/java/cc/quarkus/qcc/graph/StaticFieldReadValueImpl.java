package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.constraint.Constraint;

/**
 *
 */
final class StaticFieldReadValueImpl extends FieldOperationImpl implements FieldReadValue {
    Constraint constraint;

    public String getLabelForGraph() {
        return "get-static-field";
    }

    public Constraint getConstraint() {
        return constraint;
    }

    public void setConstraint(final Constraint constraint) {
        this.constraint = constraint;
    }
}
