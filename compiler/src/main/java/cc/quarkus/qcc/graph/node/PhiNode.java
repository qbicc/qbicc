package cc.quarkus.qcc.graph.node;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import cc.quarkus.qcc.graph.type.ConcreteType;
import cc.quarkus.qcc.graph.type.Type;
import cc.quarkus.qcc.parse.MultiData;

public class PhiNode<T extends Type> extends Node<T> {

    public PhiNode(ControlNode<?> control, T outType, MultiData local) {
        super(control, outType);
        this.local = local;
        this.id = COUNTER.incrementAndGet();
    }

    public Collection<Node<T>> values() {
        return local.values();
    }

    public void possiblySimplify() {
        if (getSuccessors().isEmpty()) {
            for (Node<?> predecessor : getPredecessors()) {
                predecessor.removeSuccessor(this);
            }
            return;
        }


        System.err.println(this.id + " possibly simplify: " + local.values().size() + " " + this.getSuccessors());
        if (local.values().size() != 1) {
            return;
        }

        Node<T> oneValue = this.local.values().iterator().next();

        for (Node<?> successor : getSuccessors()) {
            System.err.println(" replace in " + successor);
            successor.replacePredecessor(this, oneValue);
        }

        for (Node<?> predecessor : getPredecessors()) {
            predecessor.removeSuccessor(this);
        }

    }

    @Override
    public String label() {
        return "phi: " + this.id;
    }

    @Override
    public String toString() {
        return label();
    }

    private final MultiData<T> local;

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    private final int id;
}
