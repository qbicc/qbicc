package cc.quarkus.qcc.graph.node;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicInteger;

import cc.quarkus.qcc.type.TypeDescriptor;

public abstract class AbstractNode<V> implements Node<V> {

    protected AbstractNode(ControlNode<?> control, TypeDescriptor<V> outType) {
        this.outType = outType;
        this.id = COUNTER.incrementAndGet();
        this.control = control;
        if (control != null) {
            control.addSuccessor(this);
        }
    }

    protected AbstractNode(TypeDescriptor<V> outType) {
        this(null, outType);
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

    //public List<ControlNode<?>> getControlPredecessors() {
    //return this.predecessors.stream().filter(e->e instanceof ControlNode).map(e->(ControlNode<?>)e).collect(Collectors.toList());
    //}

    public List<Node<?>> getSuccessors() {
        return this.successors;
    }

    public Class<V> getType() {
        return this.outType.valueType();
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
    public boolean removeUnreachableSuccessors() {
        ListIterator<Node<?>> iter = this.successors.listIterator();
        boolean changed = false;
        while ( iter.hasNext() ) {
            Node<?> each = iter.next();
            if ( each.getSuccessors().isEmpty() && ! ( each instanceof EndNode ))  {
                changed = true;
                iter.remove();
            }
        }
        return changed;
    }

    //@Override
    //public String toString() {
        ////return label();
    //}

    protected final List<Node<?>> successors = new ArrayList<>();

    private final TypeDescriptor<V> outType;

    private final int id;

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    protected ControlNode<?> control;

    private int line = -1;
}
