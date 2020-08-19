package cc.quarkus.qcc.graph;

/**
 *
 */
final class ParameterValueImpl extends ValueImpl implements ParameterValue {
    NodeHandle type;
    int index = -1;

    ParameterValueImpl() {
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(final int idx) {
        index = idx;
    }

    public void setType(final Type type) {
        this.type = NodeHandle.of(type);
    }

    public Type getType() {
        return NodeHandle.getTargetOf(type);
    }

    public <P> void accept(GraphVisitor<P> visitor, P param) {
        visitor.visit(param, this);
    }

    public String getLabelForGraph() {
        return "param[" + index + "]";
    }
}
