package cc.quarkus.qcc.graph;

final class ArrayElementWriteImpl extends ArrayElementOperationImpl implements ArrayElementWrite {
    private NodeHandle writeValue;

    public Value getWriteValue() {
        return NodeHandle.getTargetOf(writeValue);
    }

    public void setWriteValue(final Value value) {
        writeValue = NodeHandle.of(value);
    }

    public String getLabelForGraph() {
        return "array write";
    }
}
