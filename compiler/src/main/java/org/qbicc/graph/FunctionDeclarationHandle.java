package org.qbicc.graph;

import org.qbicc.object.FunctionDeclaration;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * A handle for a function.
 */
public final class FunctionDeclarationHandle extends AbstractProgramObjectHandle {

    FunctionDeclarationHandle(ExecutableElement element, int line, int bci, FunctionDeclaration function) {
        super(element, line, bci, function);
    }

    @Override
    public boolean equals(AbstractProgramObjectHandle other) {
        return other instanceof FunctionDeclarationHandle && equals((FunctionDeclarationHandle) other);
    }

    public boolean equals(final FunctionDeclarationHandle other) {
        return super.equals(other);
    }

    @Override
    public FunctionDeclaration getProgramObject() {
        return (FunctionDeclaration) super.getProgramObject();
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
