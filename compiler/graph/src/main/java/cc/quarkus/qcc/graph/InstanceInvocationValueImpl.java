package cc.quarkus.qcc.graph;

/**
 *
 */
class InstanceInvocationValueImpl extends InvocationValueImpl implements InstanceInvocationValue {
    private NodeHandle instance;

    public Value getInstance() {
        return NodeHandle.getTargetOf(instance);
    }

    public void setInstance(final Value value) {
        instance = NodeHandle.of(value);
    }
}
