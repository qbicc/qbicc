package org.qbicc.object;

import org.qbicc.graph.literal.SymbolLiteral;
import org.qbicc.type.FunctionType;
import org.qbicc.type.definition.element.Element;

/**
 * A function definition.
 */
public final class FunctionDeclaration extends SectionObject {
    FunctionDeclaration(final Element originalElement, final String name, final SymbolLiteral literal) {
        super(originalElement, name, literal);
    }

    public FunctionType getType() {
        return (FunctionType) super.getType();
    }
}
