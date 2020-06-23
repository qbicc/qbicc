package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.constraint.Constraint;

/**
 *
 */
abstract class ValueProgramNodeImpl extends ProgramNodeImpl implements Value {
    Constraint constraint;

    String getShape() {
        return "oval";
    }

    public Constraint getConstraint() {
        return constraint;
    }

    public void setConstraint(final Constraint constraint) {
        this.constraint = constraint;
    }
}
