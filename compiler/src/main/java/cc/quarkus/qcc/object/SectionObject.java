package cc.quarkus.qcc.object;

import cc.quarkus.qcc.graph.literal.SymbolLiteral;
import cc.quarkus.qcc.type.definition.element.BasicElement;

/**
 * An object which is part of a section.
 */
public abstract class SectionObject extends ProgramObject {
    final BasicElement originalElement;

    SectionObject(final BasicElement originalElement, final String name, final SymbolLiteral literal) {
        super(name, literal);
        this.originalElement = originalElement;
    }

    public BasicElement getOriginalElement() {
        return originalElement;
    }
}
