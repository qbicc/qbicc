package cc.quarkus.qcc.graph;

/**
 *
 */
final class ThisValueImpl extends ValueImpl implements ThisValue {
    ClassType type;

    ThisValueImpl(ClassType type) {
        this.type = type;
    }

    public ClassType getType() {
        return type;
    }

    public <P> void accept(GraphVisitor<P> visitor, P param) {
        visitor.visit(param, this);
    }

    public String getLabelForGraph() {
        return "this";
    }
}
