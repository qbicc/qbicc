package cc.quarkus.qcc.object;

import cc.quarkus.qcc.graph.literal.SymbolLiteral;
import cc.quarkus.qcc.type.ValueType;

/**
 * An object which will be emitted to the final program.
 */
public abstract class ProgramObject {
    final String name;
    final SymbolLiteral literal;

    ProgramObject(final String name, final SymbolLiteral literal) {
        this.name = name;
        this.literal = literal;
    }

    public String getName() {
        return name;
    }

    public ValueType getType() {
        return literal.getType();
    }
}
