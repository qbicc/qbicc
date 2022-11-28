package org.qbicc.graph;

import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.InstanceMethodElement;

/**
 *
 */
public final class VirtualMethodLookup extends AbstractMethodLookup {

    VirtualMethodLookup(final Node callSite, final ExecutableElement element, final int line, final int bci, final Node dependency, final Value typeId, final InstanceMethodElement method) {
        super(callSite, element, line, bci, dependency, typeId, method);
    }

    @Override
    String getLabel() {
        return "virtual";
    }

    @Override
    String getNodeName() {
        return "VirtualMethodLookup";
    }

    @Override
    public boolean equals(AbstractMethodLookup other) {
        return other instanceof VirtualMethodLookup vml && equals(vml);
    }

    public boolean equals(VirtualMethodLookup other) {
        return super.equals(other);
    }

    @Override
    public <T, R> R accept(ValueVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }
}
