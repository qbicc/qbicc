package org.qbicc.plugin.native_;

import org.qbicc.type.FunctionType;
import org.qbicc.type.definition.DefinedTypeDefinition;

/**
 *
 */
final class ExternalFunctionInfo extends NativeFunctionInfo {
    private final DefinedTypeDefinition declaringClass;
    private final String name;
    private final FunctionType type;

    ExternalFunctionInfo(DefinedTypeDefinition declaringClass, String name, FunctionType type) {
        this.declaringClass = declaringClass;
        this.name = name;
        this.type = type;
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
}
