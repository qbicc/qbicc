package cc.quarkus.qcc.graph;

final class ArrayLengthValueImpl extends ValueImpl implements ArrayLengthValue {
    NodeHandle instance;

    public <P> void accept(GraphVisitor<P> visitor, P param) {
        visitor.visit(param, this);
    }

    public Value getInstance() {
        return NodeHandle.getTargetOf(instance);
    }

    public void setInstance(final Value value) {
        this.instance = NodeHandle.of(value);
    }

    public String getLabelForGraph() {
        return "length-of";
    }
}
