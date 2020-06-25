package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.constraint.Constraint;

/**
 *
 */
final class NewValueImpl extends MemoryStateValueImpl implements NewValue {
    NodeHandle type;

    public ClassType getType() {
        return NodeHandle.getTargetOf(type);
    }

    public void setType(final ClassType type) {
        this.type = NodeHandle.of(type);
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
