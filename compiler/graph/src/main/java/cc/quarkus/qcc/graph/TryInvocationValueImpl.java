package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.constraint.Constraint;
import cc.quarkus.qcc.type.definition.element.MethodElement;

class TryInvocationValueImpl extends TryInvocationImpl implements TryInvocationValue {
    Constraint constraint;

    public MethodElement getInvocationTarget() {
        return (MethodElement) super.getInvocationTarget();
    }

    public <P> void accept(GraphVisitor<P> visitor, P param) {
        visitor.visit(param, (TryInvocationValue) this);
    }

    public Constraint getConstraint() {
        return constraint;
    }

    public void setConstraint(final Constraint constraint) {
        this.constraint = constraint;
    }
}
