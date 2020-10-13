package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.constraint.Constraint;

/**
 *
 */
abstract class AbstractValue extends AbstractNode implements Value {
    public Constraint getConstraint() {
        return null;
    }
}
