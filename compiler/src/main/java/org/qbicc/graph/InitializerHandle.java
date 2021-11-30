package org.qbicc.graph;

import org.qbicc.type.FunctionType;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.descriptor.MethodDescriptor;

/**
 * A handle to an initializer, which only is invoked one time.
 */
public final class InitializerHandle extends Executable {
    InitializerHandle(ExecutableElement currentElement, int line, int bci, InitializerElement executable, MethodDescriptor callSiteDescriptor, FunctionType callSiteType) {
        super(currentElement, line, bci, executable, callSiteDescriptor, callSiteType);
    }

    @Override
    public InitializerElement getExecutable() {
        return (InitializerElement) super.getExecutable();
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
    String getNodeName() {
        return "Initializer";
    }
}
