package org.qbicc.plugin.native_;

import org.qbicc.type.FunctionType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 *
 */
final class ExternalFunctionInfo extends NativeFunctionInfo {
    private final DefinedTypeDefinition declaringClass;
    private final String name;
    private final FunctionType type;
    private final ExecutableElement originalElement;

    ExternalFunctionInfo(DefinedTypeDefinition declaringClass, String name, FunctionType type, ExecutableElement originalElement) {
        this.declaringClass = declaringClass;
        this.name = name;
        this.type = type;
        this.originalElement = originalElement;
    }

    public String getName() {
        return name;
    }

    public FunctionType getType() {
        return type;
    }

    public DefinedTypeDefinition getDeclaringClass() {
        return declaringClass;
    }

    @Override
    public ExecutableElement originalElement() {
        return originalElement;
    }
}
