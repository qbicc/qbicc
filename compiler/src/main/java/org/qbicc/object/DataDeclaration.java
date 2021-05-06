package org.qbicc.object;

import org.qbicc.graph.literal.SymbolLiteral;
import org.qbicc.type.definition.element.MemberElement;

/**
 * A declaration of some global data item.
 */
public class DataDeclaration extends SectionObject {
    DataDeclaration(final MemberElement originalElement, final String name, final SymbolLiteral literal) {
        super(originalElement, name, literal);
    }

    public MemberElement getOriginalElement() {
        return (MemberElement) super.getOriginalElement();
    }
}
