package cc.quarkus.qcc.graph;

final class TryInstanceInvocationImpl extends TryInvocationImpl implements TryInstanceInvocation {
    private NodeHandle instance;

    public Value getInstance() {
        return NodeHandle.getTargetOf(instance);
    }

    public void setInstance(final Value value) {
        instance = NodeHandle.of(value);
    }
}
