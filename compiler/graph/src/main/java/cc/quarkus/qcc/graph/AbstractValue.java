package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.constraint.Constraint;

/**
 *
 */
abstract class AbstractValue extends AbstractNode implements Value {
    AbstractValue(final int line, final int bci) {
        super(line, bci);
    }

    public Constraint getConstraint() {
        return null;
    }
}
