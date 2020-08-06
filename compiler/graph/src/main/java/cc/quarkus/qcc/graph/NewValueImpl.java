package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.constraint.Constraint;

/**
 *
 */
final class NewValueImpl extends DependentValueImpl implements NewValue {
    NodeHandle type;

    public ClassType getType() {
        return NodeHandle.getTargetOf(type);
    }

    public void setType(final ClassType type) {
        this.type = NodeHandle.of(type);
    }

    public <P> void accept(GraphVisitor<P> visitor, P param) {
        visitor.visit(param, this);
    }

    public Constraint getConstraint() {
        return null;
    }

    public void setConstraint(final Constraint constraint) {

    }

    public String getLabelForGraph() {
        return "new";
    }
}
