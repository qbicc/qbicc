package cc.quarkus.qcc.object;

import cc.quarkus.qcc.graph.literal.SymbolLiteral;
import cc.quarkus.qcc.type.FunctionType;
import cc.quarkus.qcc.type.definition.element.BasicElement;
import cc.quarkus.qcc.type.definition.element.Element;

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
