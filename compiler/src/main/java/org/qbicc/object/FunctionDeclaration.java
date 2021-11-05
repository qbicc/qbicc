package org.qbicc.object;

import org.qbicc.type.FunctionType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * A function definition.
 */
public final class FunctionDeclaration extends SectionObject {
    FunctionDeclaration(final ExecutableElement originalElement, final String name, final FunctionType functionType) {
        super(originalElement, name, functionType);
    }

    FunctionDeclaration(final Function original) {
        super(original);
    }

    public ExecutableElement getOriginalElement() {
        return (ExecutableElement) super.getOriginalElement();
    }

    @Override
    public FunctionDeclaration getDeclaration() {
        return this;
    }

    public FunctionType getValueType() {
        return (FunctionType) super.getValueType();
    }
}
