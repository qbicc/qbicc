package cc.quarkus.qcc.object;

import cc.quarkus.qcc.graph.literal.SymbolLiteral;
import cc.quarkus.qcc.type.FunctionType;
import cc.quarkus.qcc.type.definition.MethodBody;

/**
 * A function definition.
 */
public final class Function extends SectionObject {
    private volatile MethodBody body;

    Function(final String name, final SymbolLiteral literal) {
        super(name, literal);
    }

    public FunctionType getType() {
        return (FunctionType) super.getType();
    }

    public MethodBody getBody() {
        return body;
    }

    public void replaceBody(final MethodBody body) {
        this.body = body;
    }
}
