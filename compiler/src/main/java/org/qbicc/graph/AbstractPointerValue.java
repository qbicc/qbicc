package org.qbicc.graph;

import org.qbicc.type.definition.element.ExecutableElement;

abstract class AbstractPointerValue extends AbstractNode implements PointerValue {
    AbstractPointerValue(final Node callSite, final ExecutableElement element, final int line, final int bci) {
        super(callSite, element, line, bci);
    }


}
