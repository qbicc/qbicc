package org.qbicc.plugin.native_;

import org.qbicc.type.FunctionType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 *
 */
abstract class NativeFunctionInfo {

    NativeFunctionInfo() {
    }

    public abstract String getName();

    public abstract FunctionType getType();

    public abstract DefinedTypeDefinition getDeclaringClass();

    public abstract ExecutableElement originalElement();
}
