package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.constraint.Constraint;

class InvocationValueImpl extends InvocationImpl implements InvocationValue {
    Constraint cons;

    public Constraint getConstraint() {
        return cons;
    }

    public void setConstraint(final Constraint constraint) {
        this.cons = constraint;
    }
}
