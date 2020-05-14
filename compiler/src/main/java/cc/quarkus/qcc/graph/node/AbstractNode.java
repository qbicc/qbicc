package cc.quarkus.qcc.graph.node;

import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.type.QType;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;

public abstract class AbstractNode<V extends QType> implements Node<V> {

    protected AbstractNode(Graph<?> graph, ControlNode<?> control, TypeDescriptor<V> outType) {
        this.graph = graph;
        this.outType = outType;
        this.id = graph.consumeNextId();
        this.control = control;
        if (control != null) {
            control.addSuccessor(this);
        }
    }

    protected AbstractNode(Graph<?> graph, TypeDescriptor<V> outType) {
        this(graph, null, outType);
    }

    @Override
    public Graph<?> getGraph() {
        return this.graph;
    }

    public Node<V> setLine(int line) {
        this.line = line;
        return this;
    }

    @Override
    public int getLine() {
        return this.line;
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

    public List<Node<?>> getSuccessors() {
        return this.successors;
    }

    public Class<V> getType() {
        return this.outType.type();
    }

    public TypeDescriptor<V> getTypeDescriptor() {
        return this.outType;
    }

    public int getId() {
        return this.id;
    }

    public String label() {
        String n = getClass().getSimpleName();
        if (n.endsWith("Node")) {
            n = n.substring(0, n.length() - "node".length()).toLowerCase();
        } else if (n.endsWith("Projection")) {
            n = n.substring(0, n.length() - "projection".length()).toLowerCase();
        } else {
            n = n;
        }
        return n + " " + getTypeDescriptor().label();
    }

    @Override
    public String toString() {
        return label();
    }

    protected final List<Node<?>> successors = new ArrayList<>();

    private final TypeDescriptor<V> outType;

    private final int id;

    private final Graph<?> graph;

    protected ControlNode<?> control;

    private int line = -1;
}
