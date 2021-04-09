package org.qbicc.object;

import org.qbicc.graph.literal.SymbolLiteral;
import org.qbicc.type.definition.element.Element;

/**
 * A declaration of some global data item.
 */
public class DataDeclaration extends SectionObject {
    DataDeclaration(final Element originalElement, final String name, final SymbolLiteral literal) {
        super(originalElement, name, literal);
    }
}
