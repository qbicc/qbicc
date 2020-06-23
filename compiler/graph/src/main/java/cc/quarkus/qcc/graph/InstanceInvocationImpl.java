package cc.quarkus.qcc.graph;

class InstanceInvocationImpl extends InvocationImpl implements InstanceInvocation {
    private NodeHandle instance;

    public Value getInstance() {
        return NodeHandle.getTargetOf(instance);
    }

    public void setInstance(final Value value) {
        instance = NodeHandle.of(value);
    }
}
