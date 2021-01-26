package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.definition.element.ExecutableElement;

/**
 *
 */
abstract class AbstractValue extends AbstractNode implements Value {
    AbstractValue(final Node callSite, final ExecutableElement element, final int line, final int bci) {
        super(callSite, element, line, bci);
    }
}
