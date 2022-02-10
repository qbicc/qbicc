package org.qbicc.graph;

import org.qbicc.graph.atomic.AccessMode;
import org.qbicc.graph.atomic.AccessModes;
import org.qbicc.type.FunctionType;
import org.qbicc.type.PointerType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * A handle for a pointer to a static method.
 */
public final class StaticMethodPointerHandle extends AbstractValueHandle {
    private final Value staticMethodPointer;

    StaticMethodPointerHandle(ExecutableElement element, int line, int bci, Value staticMethodPointer) {
        super(null, element, line, bci);
        // check type of pointer early
        //noinspection RedundantClassCall
        FunctionType.class.cast(PointerType.class.cast(staticMethodPointer.getType()).getPointeeType());
        this.staticMethodPointer = staticMethodPointer;
    }

    @Override
    int calcHashCode() {
        return staticMethodPointer.hashCode();
    }

    @Override
    public PointerType getPointerType() {
        return (PointerType) staticMethodPointer.getType();
    }

    @Override
    public boolean isConstantLocation() {
        return staticMethodPointer.isConstant();
    }

    @Override
    public boolean isValueConstant() {
        return staticMethodPointer.isConstant();
    }

    @Override
    public AccessMode getDetectedMode() {
        return AccessModes.SingleUnshared;
    }

    public Value getStaticMethodPointer() {
        return staticMethodPointer;
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
    public boolean equals(Object other) {
        return other instanceof StaticMethodPointerHandle ph && equals(ph);
    }

    public boolean equals(StaticMethodPointerHandle other) {
        return this == other || other != null && staticMethodPointer.equals(other.staticMethodPointer);
    }

    @Override
    String getNodeName() {
        return "StaticMethodPointer";
    }
}
