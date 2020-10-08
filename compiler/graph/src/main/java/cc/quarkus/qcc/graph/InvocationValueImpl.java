package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.constraint.Constraint;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.definition.element.ParameterizedExecutableElement;

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

    public MethodElement getInvocationTarget() {
        return (MethodElement) super.getInvocationTarget();
    }

    void setInvocationTarget(final ParameterizedExecutableElement target) {
        setInvocationTarget((MethodElement) target);
    }

    void setInvocationTarget(final MethodElement target) {
        super.setInvocationTarget(target);
    }
}
