package org.qbicc.graph;

import static org.qbicc.graph.atomic.AccessModes.SingleUnshared;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.graph.atomic.AccessMode;
import org.qbicc.type.PointerType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.definition.element.FunctionElement;

/**
 * A pointer to the location where the current thread is stashed.
 */
public final class CurrentThread extends AbstractValue {

    private final PointerType pointerType;

    CurrentThread(final ProgramLocatable pl, ReferenceType valueType) {
        super(pl);
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
        return element() instanceof FunctionElement;
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
        return ! (element() instanceof FunctionElement);
    }

    public boolean isPointeeNullable() {
        return element() instanceof FunctionElement;
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
