package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.constraint.Constraint;

class TryInvocationValueImpl extends TryInvocationImpl implements TryInvocationValue {
    Constraint constraint;

    public Constraint getConstraint() {
        return constraint;
    }

    public void setConstraint(final Constraint constraint) {
        this.constraint = constraint;
    }
}
