package org.qbicc.graph;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.type.definition.element.InstanceMethodElement;

/**
 *
 */
public final class InterfaceMethodLookup extends AbstractMethodLookup {

    InterfaceMethodLookup(final ProgramLocatable pl, final Node dependency, final Value typeId, final InstanceMethodElement method) {
        super(pl, dependency, typeId, method);
    }

    @Override
    String getLabel() {
        return "interface";
    }

    @Override
    String getNodeName() {
        return "InterfaceMethodLookup";
    }

    @Override
    public boolean equals(AbstractMethodLookup other) {
        return other instanceof InterfaceMethodLookup vml && equals(vml);
    }

    public boolean equals(InterfaceMethodLookup other) {
        return super.equals(other);
    }

    @Override
    public <T, R> R accept(ValueVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }
}
