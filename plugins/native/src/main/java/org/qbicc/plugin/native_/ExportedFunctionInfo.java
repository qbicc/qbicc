package org.qbicc.plugin.native_;

import org.qbicc.type.FunctionType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FunctionElement;

/**
 *
 */
final class ExportedFunctionInfo extends NativeFunctionInfo {
    private final FunctionElement functionElement;

    ExportedFunctionInfo(FunctionElement functionElement) {
        this.functionElement = functionElement;
    }

    @Override
    public String getName() {
        return functionElement.getName();
    }

    @Override
    public FunctionType getType() {
        return functionElement.getType();
    }

    @Override
    public DefinedTypeDefinition getDeclaringClass() {
        return functionElement.getEnclosingType();
    }

    public ExecutableElement originalElement() {
        return functionElement;
    }

    public FunctionElement getFunctionElement() {
        return functionElement;
    }
}
