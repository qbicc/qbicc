package cc.quarkus.qcc.graph2;

/**
 *
 */
final class CastValueImpl extends ValueProgramNodeImpl implements CastValue {
    NodeHandle input;
    NodeHandle targetType;

    public Value getInput() {
        return NodeHandle.getTargetOf(input);
    }

    public void setInput(final Value value) {
        input = NodeHandle.of(value);
    }

    public Type getTargetType() {
        return NodeHandle.getTargetOf(targetType);
    }

    public void setTargetType(final Type targetType) {
        this.targetType = NodeHandle.of(targetType);
    }

    public String getLabelForGraph() {
        return "cast";
    }
}
