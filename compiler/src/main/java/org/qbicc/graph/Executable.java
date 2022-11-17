package org.qbicc.graph;

import static org.qbicc.graph.atomic.AccessModes.SingleUnshared;

import java.util.Objects;

import org.qbicc.graph.atomic.AccessMode;
import org.qbicc.type.InvokableType;
import org.qbicc.type.PointerType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * A value handle to an executable element.
 */
public final class Executable extends AbstractValueHandle {

    private final Value target;
    private final Value receiver;

    Executable(final ExecutableElement currentElement, final int line, final int bci, final Value target, final Value receiver) {
        super(null, currentElement, line, bci);
        this.target = target;
        this.receiver = receiver;
    }

    @Override
    int calcHashCode() {
        return Objects.hash(target, receiver);
    }

    @Override
    public PointerType getType() {
        return getPointeeType().getPointer();
    }

    @Override
    public InvokableType getPointeeType() {
        return target.getPointeeType(InvokableType.class);
    }

    @Override
    String getNodeName() {
        return "Executable";
    }

    public Value getTarget() {
        return target;
    }

    @Override
    public Value getReceiver() {
        return receiver;
    }

    @Override
    public boolean isConstantLocation() {
        return true;
    }

    @Override
    public boolean isValueConstant() {
        return true;
    }

    @Override
    public <T, R> R accept(ValueHandleVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }

    @Override
    public <T> long accept(ValueHandleVisitorLong<T> visitor, T param) {
        return visitor.visit(param, this);
    }

    @Override
    public boolean isNoThrow() {
        return target.isNoThrow();
    }

    @Override
    public boolean isNoSafePoints() {
        return target.isNoSafePoints();
    }

    @Override
    public boolean isNoReturn() {
        return target.isNoReturn();
    }

    @Override
    public boolean isFold() {
        return target.isFold();
    }

    @Override
    public final boolean equals(Object other) {
        return other instanceof Executable && equals((Executable) other);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        b.append('{');
        // todo: replace with executable.toString(b)
        target.toString(b);
        b.append('}');
        return b;
    }

    public boolean equals(Executable other) {
        return this == other || other != null && target.equals(other.target) && receiver.equals(other.receiver);
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public boolean isReadable() {
        return false;
    }

    @Override
    public AccessMode getDetectedMode() {
        return SingleUnshared;
    }
}
