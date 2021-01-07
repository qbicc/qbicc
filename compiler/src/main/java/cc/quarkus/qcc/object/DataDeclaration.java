package cc.quarkus.qcc.object;

import cc.quarkus.qcc.graph.literal.SymbolLiteral;
import cc.quarkus.qcc.type.definition.element.Element;

/**
 * A declaration of some global data item.
 */
public class DataDeclaration extends SectionObject {
    DataDeclaration(final Element originalElement, final String name, final SymbolLiteral literal) {
        super(originalElement, name, literal);
    }
}
