package org.qbicc.graph;

import org.qbicc.graph.atomic.AccessMode;
import org.qbicc.type.PointerType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FunctionElement;

import static org.qbicc.graph.atomic.AccessModes.SingleUnshared;

/**
 * A handle representing the location where the current thread is stashed.
 */
public final class CurrentThread extends AbstractValueHandle {
    private final ReferenceType valueType;

    CurrentThread(ExecutableElement element, int line, int bci, ReferenceType valueType) {
        super(null, element, line, bci);
        this.valueType = valueType;
    }

    @Override
    public ReferenceType getPointeeType() {
        return valueType;
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
        // all are equal
        return other != null;
    }

    @Override
    public PointerType getType() {
        return valueType.getPointer();
    }

    @Override
    public boolean isConstantLocation() {
        return true;
    }

    @Override
    public boolean isWritable() {
        return getElement() instanceof FunctionElement;
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        return b.append("CurrentThread");
    }

    @Override
    public boolean isReadable() {
        return true;
    }

    @Override
    public boolean isValueConstant() {
        return ! (getElement() instanceof FunctionElement);
    }

    @Override
    public AccessMode getDetectedMode() {
        return SingleUnshared;
    }

    @Override
    public <T, R> R accept(ValueHandleVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }

    @Override
    public <T> long accept(ValueHandleVisitorLong<T> visitor, T param) {
        return visitor.visit(param, this);
    }
}
