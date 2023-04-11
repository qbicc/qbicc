package org.qbicc.graph;

import org.qbicc.graph.atomic.AccessMode;
import org.qbicc.type.PointerType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FunctionElement;

import static org.qbicc.graph.atomic.AccessModes.SingleUnshared;

/**
 * A pointer to the location where the current thread is stashed.
 */
public final class CurrentThread extends AbstractValue {

    private final PointerType pointerType;

    CurrentThread(Node callSite, ExecutableElement element, int line, int bci, ReferenceType valueType) {
        super(callSite, element, line, bci);
        this.pointerType = valueType.getPointer();
    }

    @Override
    int calcHashCode() {
        return CurrentThread.class.hashCode();
    }

    @Override
    String getNodeName() {
        return "CurrentThread";
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CurrentThread ct && equals(ct);
    }

    public boolean equals(CurrentThread other) {
        return other != null;
    }

    @Override
    public PointerType getType() {
        return pointerType;
    }

    @Override
    public boolean isWritable() {
        return getElement() instanceof FunctionElement;
    }

    @Override
    StringBuilder toRValueString(StringBuilder b) {
        return b.append("current thread ptr");
    }

    @Override
    public boolean isReadable() {
        return true;
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    @Override
    public boolean isPointeeConstant() {
        return ! (getElement() instanceof FunctionElement);
    }

    public boolean isPointeeNullable() {
        return getElement() instanceof FunctionElement;
    }

    @Override
    public AccessMode getDetectedMode() {
        return SingleUnshared;
    }

    @Override
    public <T, R> R accept(ValueVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }
}
