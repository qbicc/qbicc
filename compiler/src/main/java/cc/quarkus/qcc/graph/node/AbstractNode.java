package cc.quarkus.qcc.graph.node;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractNode<V> implements Node<V> {

    protected AbstractNode(ControlNode<?> control, Class<V> outType) {
        this.outType = outType;
        this.id = COUNTER.incrementAndGet();
        this.control = control;
        if (control != null) {
            control.addSuccessor(this);
        }
    }

    protected AbstractNode(Class<V> outType) {
        this(null, outType);
    }

    @Override
    public ControlNode<?> getControl() {
        return this.control;
    }

    @Override
    public void setControl(ControlNode<?> control) {
        if (this.control != null) {
            throw new RuntimeException("attempting to replace control input");
        }
        this.control = control;
    }

    @Override
    public void addSuccessor(Node<?> out) {
        this.successors.add(out);
    }

    public abstract List<? extends Node<?>> getPredecessors();

    //public List<ControlNode<?>> getControlPredecessors() {
    //return this.predecessors.stream().filter(e->e instanceof ControlNode).map(e->(ControlNode<?>)e).collect(Collectors.toList());
    //}

    public List<Node<?>> getSuccessors() {
        return this.successors;
    }

    public Class<V> getType() {
        return this.outType;
    }

    public int getId() {
        return this.id;
    }

    public String label() {
        String n = this.id + ": " + getClass().getSimpleName();
        if (n.endsWith("Node")) {
            return n.substring(0, n.length() - "node".length()).toLowerCase();
        } else if (n.endsWith("Projection")) {
            return n.substring(0, n.length() - "projection".length()).toLowerCase();
        } else {
            return n;
        }
    }

    //@Override
    //public String toString() {
        ////return label();
    //}

    private final List<Node<?>> successors = new ArrayList<>();

    private final Class<V> outType;

    private final int id;

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    private ControlNode<?> control;
}
