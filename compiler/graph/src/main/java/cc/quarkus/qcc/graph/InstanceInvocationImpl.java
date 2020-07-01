package cc.quarkus.qcc.graph;

class InstanceInvocationImpl extends InvocationImpl implements InstanceInvocation {
    private NodeHandle instance;
    private Kind kind = Kind.EXACT;

    public Value getInstance() {
        return NodeHandle.getTargetOf(instance);
    }

    public void setInstance(final Value value) {
        instance = NodeHandle.of(value);
    }

    public Kind getKind() {
        return kind;
    }

    public void setKind(final Kind kind) {
        this.kind = kind;
    }
}
