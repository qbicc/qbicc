package cc.quarkus.qcc.graph;

import io.smallrye.common.constraint.Assert;

abstract class ArrayElementOperationImpl extends MemoryStateImpl implements ArrayElementOperation {
    private NodeHandle instance;
    private NodeHandle index;
    private Mode mode = Mode.PLAIN;

    public Value getIndex() {
        return NodeHandle.getTargetOf(index);
    }

    public void setIndex(final Value value) {
        index = NodeHandle.of(value);
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(final Mode mode) {
        this.mode = Assert.checkNotNullParam("mode", mode);
    }

    public Value getInstance() {
        return NodeHandle.getTargetOf(instance);
    }

    public void setInstance(final Value value) {
        instance = NodeHandle.of(value);
    }
}
