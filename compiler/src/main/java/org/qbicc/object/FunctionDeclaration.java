package org.qbicc.object;

import org.qbicc.graph.literal.SymbolLiteral;
import org.qbicc.type.FunctionType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * A function definition.
 */
public final class FunctionDeclaration extends SectionObject {
    FunctionDeclaration(final ExecutableElement originalElement, final String name, final SymbolLiteral literal) {
        super(originalElement, name, literal);
    }

    public ExecutableElement getOriginalElement() {
        return (ExecutableElement) super.getOriginalElement();
    }

    public FunctionType getType() {
        return (FunctionType) super.getType();
    }
}
