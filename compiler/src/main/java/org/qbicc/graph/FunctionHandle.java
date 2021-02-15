package org.qbicc.graph;

import org.qbicc.object.Function;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * A handle for a function.
 */
public final class FunctionHandle extends AbstractProgramObjectHandle {

    FunctionHandle(ExecutableElement element, int line, int bci, Function function) {
        super(element, line, bci, function);
    }

    @Override
    public boolean equals(AbstractProgramObjectHandle other) {
        return other instanceof FunctionHandle && equals((FunctionHandle) other);
    }

    public boolean equals(final FunctionHandle other) {
        return super.equals(other);
    }

    @Override
    public Function getProgramObject() {
        return (Function) super.getProgramObject();
    }

    @Override
    public MemoryAtomicityMode getDetectedMode() {
        return MemoryAtomicityMode.NONE;
    }

    @Override
    public <T, R> R accept(ValueHandleVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }

    public <T> long accept(final ValueHandleVisitorLong<T> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
