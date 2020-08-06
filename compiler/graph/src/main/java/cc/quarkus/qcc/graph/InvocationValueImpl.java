package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.constraint.Constraint;

class InvocationValueImpl extends InvocationImpl implements InvocationValue {
    Constraint cons;

    public <P> void accept(GraphVisitor<P> visitor, P param) {
        visitor.visit(param, this);
    }

    public Constraint getConstraint() {
        return cons;
    }

    public void setConstraint(final Constraint constraint) {
        this.cons = constraint;
    }
}
