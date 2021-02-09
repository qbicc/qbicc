package cc.quarkus.qcc.object;

import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.literal.SymbolLiteral;
import cc.quarkus.qcc.type.definition.element.Element;

/**
 * A data object definition.
 */
public final class Data extends SectionObject {
    private volatile Value value;
    private volatile DataDeclaration declaration;

    Data(final Element originalElement, final String name, final SymbolLiteral symbolLiteral, final Value value) {
        super(originalElement, name, symbolLiteral);
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public Value getValue() {
        return value;
    }

    public void setValue(final Value value) {
        this.value = value;
    }

    public DataDeclaration getDeclaration() {
        DataDeclaration declaration = this.declaration;
        if (declaration == null) {
            synchronized (this) {
                declaration = this.declaration;
                if (declaration == null) {
                    declaration = this.declaration = new DataDeclaration(originalElement, name, literal);
                }
            }
        }
        return declaration;
    }
}
