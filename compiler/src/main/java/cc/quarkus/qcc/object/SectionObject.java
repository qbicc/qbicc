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

    /**
     * The program-level element that caused this SectionObject to be generated.
     * If the SectionObject is synthetic (injected by/for the Qcc runtime) originalElement will be null
     */
    public Element getOriginalElement() {
        return originalElement;
    }
}
