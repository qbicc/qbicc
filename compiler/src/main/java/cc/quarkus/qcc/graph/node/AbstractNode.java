package cc.quarkus.qcc.graph.node;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import cc.quarkus.qcc.graph.type.Type;
import cc.quarkus.qcc.graph.type.Value;
import cc.quarkus.qcc.interpret.Context;

public abstract class AbstractNode<T extends Type<T>, V extends Value<T, V>> implements Node<T, V> {

    protected AbstractNode(ControlNode<?, ?> control, T outType) {
        this.outType = outType;
        this.id = COUNTER.incrementAndGet();
        this.control = control;
        if (control != null) {
            control.addSuccessor(this);
        }
    }

    protected AbstractNode(T outType) {
        this(null, outType);
    }

    @Override
    public ControlNode<?, ?> getControl() {
        return this.control;
    }

    @Override
    public void setControl(ControlNode<?, ?> control) {
        if (this.control != null) {
            throw new RuntimeException("attempting to replace control input");
        }
        this.control = control;
    }

    /*
    protected void addPredecessor(Node<?,?> in) {
        this.predecessors.add(in);
        in.addSuccessor(this);
    }
    */

    @Override
    public void addSuccessor(Node<?, ?> out) {
        this.successors.add(out);
    }


    /*
    public <T extends Type<?>> AbstractNode<T> tryCoerce(Type<?> type) {
        if ( type == getType() ) {
            return (AbstractNode<T>) this;
        }
        return type.coerce(this);
    }
     */

    /*
    public V getValue(Context context)  {
        return null;
    }
     */

    public abstract List<? extends Node<?, ?>> getPredecessors();

    //public List<ControlNode<?>> getControlPredecessors() {
    //return this.predecessors.stream().filter(e->e instanceof ControlNode).map(e->(ControlNode<?>)e).collect(Collectors.toList());
    //}

    public List<Node<?, ?>> getSuccessors() {
        return this.successors;
    }

    public List<ControlNode<?, ?>> getControlSuccessors() {
        return this.successors.stream().filter(e -> e instanceof AbstractControlNode).map(e -> (ControlNode<?, ?>) e).collect(Collectors.toList());
    }

    public T getType() {
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

    @Override
    public String toString() {
        return label();
    }

    //private final List<Node<?,?>> predecessors = new ArrayList<>();
    private final List<Node<?, ?>> successors = new ArrayList<>();

    private final T outType;

    private final int id;

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    private ControlNode<?, ?> control;
}
