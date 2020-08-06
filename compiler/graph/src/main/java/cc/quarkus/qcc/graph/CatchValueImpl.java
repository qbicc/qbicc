package cc.quarkus.qcc.graph;

/**
 *
 */
final class CatchValueImpl extends ValueImpl implements CatchValue {

    public <P> void accept(GraphVisitor<P> visitor, P param) {
        visitor.visit(param, this);
    }

    public String getLabelForGraph() {
        return "catch value";
    }

    public Type getType() {
        // todo: exception type
        return Type.S32;
    }
}
