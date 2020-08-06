package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.constraint.Constraint;

final class NewArrayValueImpl extends DependentValueImpl implements NewArrayValue {
    NodeHandle type;
    NodeHandle size;

    public ArrayClassType getType() {
        return NodeHandle.getTargetOf(type);
    }

    public void setType(final ArrayClassType type) {
        this.type = NodeHandle.of(type);
    }

    public Value getSize() {
        return NodeHandle.getTargetOf(size);
    }

    public void setSize(final Value size) {
        this.size = NodeHandle.of(size);
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
        return "new array";
    }
}
