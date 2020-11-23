package cc.quarkus.qcc.object;

import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.literal.SymbolLiteral;

/**
 * A data object definition.
 */
public final class Data extends SectionObject {
    private volatile Value value;

    Data(final String name, final SymbolLiteral symbolLiteral, final Value value) {
        super(name, symbolLiteral);
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
}
