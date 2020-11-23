package cc.quarkus.qcc.object;

import cc.quarkus.qcc.graph.literal.SymbolLiteral;

/**
 * An object which is part of a section.
 */
public abstract class SectionObject extends ProgramObject {
    SectionObject(final String name, final SymbolLiteral literal) {
        super(name, literal);
    }
}
