package cc.quarkus.qcc.graph;

/**
 *
 */
abstract class CastValueImpl extends ValueImpl implements CastValue {
    NodeHandle input;
    NodeHandle targetType;

    public Value getInput() {
        return NodeHandle.getTargetOf(input);
    }

    public void setInput(final Value value) {
        input = NodeHandle.of(value);
    }

    public Type getType() {
        return NodeHandle.getTargetOf(targetType);
    }

    public void setType(final Type targetType) {
        this.targetType = NodeHandle.of(targetType);
    }

    public String getLabelForGraph() {
        return "cast";
    }
}
