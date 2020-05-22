package cc.quarkus.qcc.graph2;

final class NodeHandle {
    private Object target;

    void setTarget(Node target) {
        //noinspection RedundantCast
        this.target = (NodeImpl) target;
    }

    void setTarget(NodeHandle target) {
        Object oldTarget = this.target;
        if (oldTarget instanceof NodeHandle) {
            ((NodeHandle) oldTarget).setTarget(target);
        } else {
            this.target = target;
        }
    }

    @SuppressWarnings("unchecked")
    <N extends Node> N getTarget() {
        return (N) lastHandle().target;
    }

    NodeHandle lastHandle() {
        Object target = this.target;
        return target instanceof Node ? this : ((NodeHandle) target).lastHandle();
    }

    // helper
    static NodeHandle of(Node node) {
        return node == null ? null : ((NodeImpl) node).getHandle().lastHandle();
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
