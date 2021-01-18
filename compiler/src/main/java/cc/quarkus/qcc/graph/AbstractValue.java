package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.constraint.Constraint;
import cc.quarkus.qcc.type.definition.element.Element;

/**
 *
 */
abstract class AbstractValue extends AbstractNode implements Value {
    AbstractValue(final Element element, final int line, final int bci) {
        super(element, line, bci);
    }

    public Constraint getConstraint() {
        return null;
    }
}
