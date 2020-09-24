package cc.quarkus.qcc.graph;

public final class NodeHandle {
    private Object target;

    public NodeHandle() {
    }

    public <N extends Node> N setTarget(N target) {
        NodeImpl nodeTarget = (NodeImpl) target;
        if (nodeTarget.getHandleIfExists() == null) {
            nodeTarget.setHandle(this);
        }
        this.target = nodeTarget;
        return target;
    }

    public NodeHandle setTarget(NodeHandle target) {
        if (target == this) {
            return target;
        }
        Object oldTarget = this.target;
        if (oldTarget instanceof NodeHandle) {
            ((NodeHandle) oldTarget).setTarget(target);
        } else {
            this.target = target;
        }
        return target;
    }

    public NodeHandle lastHandle() {
        Object target = this.target;
        if (target instanceof NodeHandle) {
            return ((NodeHandle) target).lastHandle();
        } else if (target == null || target instanceof NodeImpl) {
            return this;
        } else {
            throw new IllegalStateException();
        }
    }

    public boolean hasTarget() {
        return lastHandle().target instanceof NodeImpl;
    }

    // helper
    public static NodeHandle of(Node node) {
        return node == null ? null : ((NodeImpl) node).getHandle().lastHandle();
    }

    @SuppressWarnings("unchecked")
    public static <N extends Node> N getTargetOf(NodeHandle handle) {
        return handle == null ? null : (N) handle.lastHandle().target;
    }

    public String toString() {
        Object target = this.target;
        if (target == null) {
            return "empty";
        } else if (target instanceof NodeHandle) {
            return "fwd to " + target;
        } else {
            return "-> " + target;
        }
    }
}
