package org.qbicc.graph;

import org.qbicc.type.FunctionType;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FunctionElement;

/**
 * A handle for a function.
 */
public final class FunctionElementHandle extends Executable {

    FunctionElementHandle(ExecutableElement element, int line, int bci, FunctionElement functionElement) {
        super(element, line, bci, functionElement, functionElement.getDescriptor(), functionElement.getType());
    }

    @Override
    public FunctionElement getExecutable() {
        return (FunctionElement) super.getExecutable();
    }

    public boolean equals(final Executable other) {
        return other instanceof FunctionElementHandle && equals((FunctionElementHandle) other);
    }

    public boolean equals(final FunctionElementHandle other) {
        return super.equals(other);
    }

    @Override
    public FunctionType getValueType() {
        return (FunctionType) super.getValueType();
    }

    @Override
    public FunctionType getCallSiteType() {
        return (FunctionType) super.getCallSiteType();
    }

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

    public <T> long accept(final ValueHandleVisitorLong<T> visitor, final T param) {
        return visitor.visit(param, this);
    }

    @Override
    String getNodeName() {
        return "Function";
    }
}
