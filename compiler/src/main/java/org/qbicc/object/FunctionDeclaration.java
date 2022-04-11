package org.qbicc.object;

import org.qbicc.type.FunctionType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * A function declaration.
 */
public final class FunctionDeclaration extends Declaration {

    FunctionDeclaration(final ExecutableElement originalElement, ProgramModule programModule, final String name, final FunctionType functionType) {
        super(originalElement, programModule, name, functionType);
    }

    FunctionDeclaration(final Function original) {
        super(original);
    }

    @Override
    public FunctionDeclaration getDeclaration() {
        return this;
    }

    public FunctionType getValueType() {
        return (FunctionType) super.getValueType();
    }
}
