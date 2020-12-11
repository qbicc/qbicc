package cc.quarkus.qcc.object;

import cc.quarkus.qcc.graph.literal.SymbolLiteral;
import cc.quarkus.qcc.type.definition.element.Element;

/**
 * An object which is part of a section.
 */
public abstract class SectionObject extends ProgramObject {
    final Element originalElement;

    SectionObject(final Element originalElement, final String name, final SymbolLiteral literal) {
        super(name, literal);
        this.originalElement = originalElement;
    }

    public Element getOriginalElement() {
        return originalElement;
    }
}
